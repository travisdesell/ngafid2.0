echo "Getting airsync token"
url='https://service-dev.air-sync.com/partner_api/v1/auth/'
hash='SkY3WVkxSzZZR0pNVERUN1lSRFZKRUJROEdYS0pQWkQ6NTU2WUVKNjBTRTNOSUVEUFdPTlhLUDVNMkdIUFRIVFZLT05LMU9FV1hSM1kzOTRNWkdUTktQWUM0UENLOE1QVg=='
echo "Posting to URL: ${url}"
curl -X POST -v $url \
    -H "Authorization: Basic $hash"
