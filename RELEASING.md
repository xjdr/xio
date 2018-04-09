
# prepare the release

```
$ mvn release:clean release:prepare
```

# diff the result if it looks good, push

```
$ git push --follow-tags origin master 
```

# did you remember to setup your bintray credentials?

```
M-C02TN11MGTDX:xio bvjp$ cat ~/.m2/settings.xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>bintray-xio-deploy</id>
      <username>BINTRAY_USER_NAME</username>
      <password>BINTRAY_API_KEY</password>
    </server>
  </servers>
</settings>
```

# build jars and push to bintray

```
$ mvn release:perform
```
