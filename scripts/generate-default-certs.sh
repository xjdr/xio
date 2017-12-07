#!/bin/bash

CA_RSA_FILE="xio-default-snakeoil-ca-rsa.pem"
CA_CRT_FILE="xio-default-snakeoil-ca-x509.pem"
CA_SUBJECT="/C=US/ST=Illinois/L=Chicago/O=Snake Oil/OU=InfoSec/CN=ca.snakeoil.com"

INT_RSA_FILE="xio-default-snakeoil-intermediate-rsa.pem"
INT_CRT_FILE="xio-default-snakeoil-intermediate-x509.pem"
INT_CSR_FILE="xio-default-snakeoil-intermediate-csr.pem"
INT_SUBJECT="/C=US/ST=Illinois/L=Chicago/O=Snake Oil/OU=InfoSec/CN=intermediate.snakeoil.com"

SERVER_KEY_FILE="xio-default-server-private-key-pkcs8.pem"
SERVER_RSA_FILE="xio-default-server-private-key-rsa.pem"
SERVER_CRT_FILE="xio-default-server-certificate-x509.pem"
SERVER_CSR_FILE="xio-default-server-certificate-csr.pem"
SERVER_SUBJECT="/C=US/ST=Illinois/L=Chicago/O=Example LLC/OU=InfoSec/CN=server.example.com"

CLIENT_KEY_FILE="xio-default-client-private-key-pkcs8.pem"
CLIENT_RSA_FILE="xio-default-client-private-key-rsa.pem"
CLIENT_CRT_FILE="xio-default-client-certificate-x509.pem"
CLIENT_CSR_FILE="xio-default-client-certificate-csr.pem"
CLIENT_SUBJECT="/C=US/ST=Illinois/L=Chicago/O=Example LLC/OU=InfoSec/CN=client.example.com"

mkdir -p target/snakeoil-ca

pushd target/snakeoil-ca

mkdir -p certs csr newcerts private
chmod 700 private
touch index.txt
echo 1000 > serial

mkdir -p intermediate/newcerts
touch intermediate/index.txt
echo 1000 > intermediate/serial

echo "Creating config"
cat <<"EOF" | sed -e "s#PWD#$PWD#g" > openssl.cnf
# OpenSSL root CA configuration file.
# https://jamielinux.com/docs/openssl-certificate-authority/appendix/root-configuration-file.html

[ ca ]
# `man ca`
default_ca = CA_default

[ CA_intermediate ]
dir               = PWD/intermediate
private_key       = PWD/private/intermediate.key.pem
certificate       = PWD/certs/intermediate.cert.pem
crl               = PWD/crl/intermediate.crl.pem
policy            = policy_loose
crl_dir           = PWD/crl
new_certs_dir     = $dir/newcerts
database          = $dir/index.txt
serial            = $dir/serial
RANDFILE          = PWD/private/.rand

[ CA_default ]
# Directory and file locations.
dir               = PWD
certs             = $dir/certs
crl_dir           = $dir/crl
new_certs_dir     = $dir/newcerts
database          = $dir/index.txt
serial            = $dir/serial
RANDFILE          = $dir/private/.rand

# The root key and root certificate.
private_key       = $dir/private/ca.key.pem
certificate       = $dir/certs/ca.cert.pem

# For certificate revocation lists.
crlnumber         = $dir/crlnumber
crl               = $dir/crl/ca.crl.pem
crl_extensions    = crl_ext
default_crl_days  = 30

# SHA-1 is deprecated, so use SHA-2 instead.
default_md        = sha256

name_opt          = ca_default
cert_opt          = ca_default
default_days      = 375
preserve          = no
policy            = policy_strict

[ policy_strict ]
# The root CA should only sign intermediate certificates that match.
# See the POLICY FORMAT section of `man ca`.
countryName             = match
stateOrProvinceName     = match
organizationName        = match
organizationalUnitName  = optional
commonName              = supplied
emailAddress            = optional

[ policy_loose ]
# Allow the intermediate CA to sign a more diverse range of certificates.
# See the POLICY FORMAT section of the `ca` man page.
countryName             = optional
stateOrProvinceName     = optional
localityName            = optional
organizationName        = optional
organizationalUnitName  = optional
commonName              = supplied
emailAddress            = optional

[ req ]
# Options for the `req` tool (`man req`).
default_bits        = 2048
distinguished_name  = req_distinguished_name
string_mask         = utf8only

# SHA-1 is deprecated, so use SHA-2 instead.
default_md          = sha256

# Extension to add when the -x509 option is used.
x509_extensions     = v3_ca

[ req_distinguished_name ]
# See <https://en.wikipedia.org/wiki/Certificate_signing_request>.
countryName                     = Country Name (2 letter code)
stateOrProvinceName             = State or Province Name
localityName                    = Locality Name
0.organizationName              = Organization Name
organizationalUnitName          = Organizational Unit Name
commonName                      = Common Name
emailAddress                    = Email Address

# Optionally, specify some defaults.
countryName_default             = GB
stateOrProvinceName_default     = England
localityName_default            =
0.organizationName_default      = Alice Ltd
organizationalUnitName_default  =
emailAddress_default            =

[ v3_ca ]
# Extensions for a typical CA (`man x509v3_config`).
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = critical, CA:true
keyUsage = critical, digitalSignature, cRLSign, keyCertSign

[ v3_intermediate_ca ]
# Extensions for a typical intermediate CA (`man x509v3_config`).
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = critical, CA:true, pathlen:0
keyUsage = critical, digitalSignature, cRLSign, keyCertSign

[ usr_cert ]
# Extensions for client certificates (`man x509v3_config`).
basicConstraints = CA:FALSE
nsCertType = client, email
nsComment = "OpenSSL Generated Client Certificate"
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer
keyUsage = critical, nonRepudiation, digitalSignature, keyEncipherment
extendedKeyUsage = clientAuth, emailProtection

[ server_cert ]
# Extensions for server certificates (`man x509v3_config`).
basicConstraints = CA:FALSE
nsCertType = server
nsComment = "OpenSSL Generated Server Certificate"
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer:always
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth

[ crl_ext ]
# Extension for CRLs (`man x509v3_config`).
authorityKeyIdentifier=keyid:always

[ ocsp ]
# Extension for OCSP signing certificates (`man ocsp`).
basicConstraints = CA:FALSE
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer
keyUsage = critical, digitalSignature
extendedKeyUsage = critical, OCSPSigning
EOF
echo "Done"

echo "Generating CA"
# generate CA
openssl req -x509 -nodes -days 3650 \
        -newkey rsa:2048 -keyout private/$CA_RSA_FILE \
        -out certs/$CA_CRT_FILE -subj "$CA_SUBJECT"

echo "Generating Intermediate"
# generate intermediate
openssl req -new -nodes -days 3650 \
        -newkey rsa:2048 -keyout private/$INT_RSA_FILE \
        -out csr/$INT_CSR_FILE -subj "$INT_SUBJECT"

# sign intermediate
openssl ca -config openssl.cnf \
        -extensions v3_intermediate_ca \
        -days 3650 -md sha256 -notext -batch \
        -cert certs/$CA_CRT_FILE \
        -keyfile private/$CA_RSA_FILE \
        -in csr/$INT_CSR_FILE \
        -out certs/$INT_CRT_FILE

echo "Generating Server"
# generate server
openssl req -new -nodes -days 3650 \
        -newkey rsa:2048 -keyout private/$SERVER_RSA_FILE \
        -out csr/$SERVER_CSR_FILE -subj "$SERVER_SUBJECT"

# sign server
openssl ca -config openssl.cnf -name CA_intermediate \
        -extensions v3_intermediate_ca \
        -days 3650 -md sha256 -notext -batch \
        -cert certs/$INT_CRT_FILE \
        -keyfile private/$INT_RSA_FILE \
        -in csr/$SERVER_CSR_FILE \
        -out certs/$SERVER_CRT_FILE

openssl pkcs8 -in private/$SERVER_RSA_FILE -inform PEM \
        -out private/$SERVER_KEY_FILE -outform PEM -topk8 -passout pass: -nocrypt

echo "Generating Client"
# generate client
openssl req -new -nodes -days 3650 \
        -newkey rsa:2048 -keyout private/$CLIENT_RSA_FILE \
        -out csr/$CLIENT_CSR_FILE -subj "$CLIENT_SUBJECT"

# sign client
openssl ca -config openssl.cnf -name CA_intermediate \
        -extensions v3_intermediate_ca \
        -days 3650 -md sha256 -notext -batch \
        -cert certs/$INT_CRT_FILE \
        -keyfile private/$INT_RSA_FILE \
        -in csr/$CLIENT_CSR_FILE \
        -out certs/$CLIENT_CRT_FILE

openssl pkcs8 -in private/$CLIENT_RSA_FILE -inform PEM \
        -out private/$CLIENT_KEY_FILE -outform PEM -topk8 -passout pass: -nocrypt

popd

echo "Copying certs"
cp -v target/snakeoil-ca/private/$SERVER_KEY_FILE target/snakeoil-ca/private/$CLIENT_KEY_FILE target/snakeoil-ca/certs/*.pem src/main/resources

echo "Setup example files"
openssl pkcs12 -export -out snakeoil-ca.p12 -inkey target/snakeoil-ca/private/$CA_RSA_FILE -in target/snakeoil-ca/certs/$CA_CRT_FILE -passout pass:""
openssl pkcs12 -export -out snakeoil-intermediate.p12 -chain -CAfile target/snakeoil-ca/certs/$CA_CRT_FILE -inkey target/snakeoil-ca/private/$INT_RSA_FILE -in target/snakeoil-ca/certs/$INT_CRT_FILE -passout pass:""
cat target/snakeoil-ca/certs/$SERVER_CRT_FILE target/snakeoil-ca/certs/$INT_CRT_FILE target/snakeoil-ca/certs/$CA_CRT_FILE  > example-certificate-chain.pem
cp target/snakeoil-ca/private/$SERVER_KEY_FILE example-private-key.pem
openssl pkcs12 -in snakeoil-intermediate.p12 -passin pass: -password pass: -passout pass:
