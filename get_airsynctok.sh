echo "Getting airsync token"
url='<Omitted>'
hash='<Omitted>'
echo "Posting to URL: ${url}"
curl -X POST -v $url \
    -H "Authorization: Basic $hash"
