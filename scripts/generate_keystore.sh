#!/bin/bash
# Script to generate a new keystore for release signing

KEYSTORE_FILE="release.keystore.jks"
KEY_ALIAS="composeapp"
VALIDITY_DAYS=10000

echo "====================================="
echo "Android Keystore Generation Script"
echo "====================================="
echo ""

if [ -f "$KEYSTORE_FILE" ]; then
    echo "Warning: Keystore file '$KEYSTORE_FILE' already exists."
    read -p "Do you want to overwrite it? (yes/no): " OVERWRITE
    if [ "$OVERWRITE" != "yes" ]; then
        echo "Exiting without creating a new keystore."
        exit 0
    fi
    rm "$KEYSTORE_FILE"
fi

echo "Generating keystore for Android app signing..."
echo ""
echo "You will be prompted to enter:"
echo "  - Keystore password (remember this!)"
echo "  - Key password (can be same as keystore password)"
echo "  - Your name, organizational unit, organization, city, state, country code"
echo ""

keytool -genkey -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity $VALIDITY_DAYS

if [ $? -eq 0 ]; then
    echo ""
    echo "====================================="
    echo "Keystore created successfully!"
    echo "====================================="
    echo ""
    echo "Keystore file: $KEYSTORE_FILE"
    echo "Key alias: $KEY_ALIAS"
    echo ""
    echo "Next steps:"
    echo "1. Copy keystore.properties.example to keystore.properties"
    echo "2. Update keystore.properties with:"
    echo "   storeFile=$KEYSTORE_FILE"
    echo "   keyAlias=$KEY_ALIAS"
    echo "   storePassword=<your_keystore_password>"
    echo "   keyPassword=<your_key_password>"
    echo ""
    echo "IMPORTANT: Keep your keystore file and passwords secure!"
    echo "Store them in a safe location and never commit them to version control."
else
    echo ""
    echo "Error: Keystore generation failed."
    exit 1
fi
