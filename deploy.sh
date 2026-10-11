#!/bin/bash

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# 接続先は .env の PHONE_IP で設定する（.env は git 管理外。雛形は .env.example）。
# 環境変数 PHONE_IP が指定されている場合はそちらを優先する。
ENV_PHONE_IP="$PHONE_IP"
if [ -f "${PROJECT_DIR}/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "${PROJECT_DIR}/.env"
    set +a
fi
PHONE_IP="${ENV_PHONE_IP:-$PHONE_IP}"

if [ -z "$PHONE_IP" ]; then
    echo "❌ PHONE_IP が未設定です。${PROJECT_DIR}/.env に PHONE_IP=<端末のIP> を書いてください（雛形: .env.example）。"
    exit 1
fi

usage() {
    echo "Usage: $0 [debug|release]"
    echo "  debug   : debug APK をビルドして ${PHONE_IP} へインストール（既定）"
    echo "  release : release APK をビルドして ${PHONE_IP} へインストール"
    echo "接続先は .env の PHONE_IP で設定します（環境変数 PHONE_IP が優先）。"
    exit 1
}

VARIANT="${1:-debug}"
case "$VARIANT" in
    debug)   GRADLE_TASK=":app:assembleDebug" ;;
    release) GRADLE_TASK=":app:assembleRelease" ;;
    *)
        echo "❌ エラー: 不正な引数です。 [ $1 ]"
        usage
        ;;
esac
APK_DIR="${PROJECT_DIR}/app/build/outputs/apk/${VARIANT}"
TARGET_IP="$PHONE_IP"

echo "🔨 ビルド中: ${GRADLE_TASK}"
if ! "${PROJECT_DIR}/gradlew" -p "$PROJECT_DIR" -q "$GRADLE_TASK"; then
    echo "❌ ビルドに失敗しました。"
    exit 1
fi

echo "📱 ターゲット: スマートフォン (${TARGET_IP})"

# 前回セッションの古い接続が adb connect の挙動を紛らわしくするため、先に切断する
echo "🧹 既存の接続をクリーンアップ中..."
for OLD_CONN in $(adb devices | grep "^${TARGET_IP}:" | awk '{print $1}'); do
    echo "  切断: ${OLD_CONN}"
    adb disconnect "$OLD_CONN" > /dev/null 2>&1
done

# ワイヤレスデバッグのポートは毎回変わる。ポートスキャンは対象機に負荷をかけるため行わず、
# mDNS (adb-tls-connect) の自己広報を avahi-browse で受動的に読み取る
MAX_MDNS_ATTEMPTS=5
CONNECTED_PORT=""

for ATTEMPT in $(seq 1 $MAX_MDNS_ATTEMPTS); do
    echo "🔄 mDNSでワイヤレスデバッグポートを検索中...（試行 ${ATTEMPT}/${MAX_MDNS_ATTEMPTS}）"

    # avahi-browse -p: フィールドは =;iface;proto;name;service;domain;hostname;address;port;txt
    CANDIDATE_PORT=$(timeout 5 avahi-browse -p -r -t _adb-tls-connect._tcp 2>/dev/null \
        | awk -F';' -v ip="$TARGET_IP" '$1=="=" && $3=="IPv4" && $8==ip {print $9; exit}')

    if [ -z "$CANDIDATE_PORT" ]; then
        echo "⚠️  ${TARGET_IP} の adb-tls-connect サービスが見つかりませんでした。"
        sleep 1
        continue
    fi

    echo "🔌 adb connect ${TARGET_IP}:${CANDIDATE_PORT} を試行中..."
    adb disconnect "${TARGET_IP}:${CANDIDATE_PORT}" > /dev/null 2>&1
    adb connect "${TARGET_IP}:${CANDIDATE_PORT}"

    # connect が成功表示でも TLS ハンドシェイク未完了で offline のことがあるため状態で確認する
    STATE=$(adb devices | grep "^${TARGET_IP}:${CANDIDATE_PORT}[[:space:]]" | awk '{print $2}')
    if [[ "$STATE" == "device" ]]; then
        CONNECTED_PORT="$CANDIDATE_PORT"
        break
    fi
    echo "⚠️  ポート ${CANDIDATE_PORT} での接続が完了しませんでした（状態: ${STATE:-なし}）。再検索します。"
    adb disconnect "${TARGET_IP}:${CANDIDATE_PORT}" > /dev/null 2>&1
done

if [ -z "$CONNECTED_PORT" ]; then
    echo "❌ ADB接続に失敗しました。"
    echo "ℹ️  端末の「ワイヤレスデバッグ」がONか、avahi-daemon が起動しているか確認してください。"
    exit 1
fi
echo "✅ ポート ${CONNECTED_PORT} で接続できました。"

APK_PATH=$(ls "${APK_DIR}"/*.apk 2>/dev/null | head -n 1)
if [ -z "$APK_PATH" ]; then
    echo "❌ APKファイルが見つかりません: ${APK_DIR}"
    exit 1
fi

echo "🚀 APKをインストール中: $(basename "$APK_PATH")"
if ! adb -s "${TARGET_IP}:${CONNECTED_PORT}" install -r "$APK_PATH"; then
    echo "❌ インストールに失敗しました。"
    exit 1
fi

echo "🎉 インストールが完了しました！"
