### Android改动
1. ExoPlayer更新到2.15.0，解决onPlayerError()不回调
2. 将gradle降到6.1.1，gradle plugin降到3.6.4，解决Java8问题
3. 将complieSdkVersion降到30，因为31需要gradle7.0
4. 新增`NoProxyDefaultHttpDataSource`类，禁止抓包
5. 新增download文件夹