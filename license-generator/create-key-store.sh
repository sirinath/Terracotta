#!/bin/bash

keytool -genkey -keyalg RSA -alias terracotta -keystore resources/keystore.jks -keypass terracotta -storepass terracotta -validity 36500

