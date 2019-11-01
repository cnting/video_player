//
//  VideoPlayerPluginManager.h
//  video_player
//
//  Created by 牛新怀 on 2019/10/21.
//

#import <Foundation/Foundation.h>


@interface VideoPlayerPluginManager : NSObject

@property (readonly, nonatomic, strong) NSString * playerUrl;
@property (readonly, nonatomic, strong) NSString * spliceOriginUrl; //主拼接链接
@property (readonly, nonatomic, strong) NSArray * resolutionArray; // 分辨率数组
@property (readonly, nonatomic, strong) NSArray * resolutionDownloadUrlArray; //对应分辨率下载数组

- (instancetype)initWithOriginPlayerUrl:(NSString *)url;

/**
 获取对应url下的分辨率和下载链接(已下载除外)

 @param urlStr 播放url
 @return dictionary
 */
+ (NSDictionary *)getM3U8AllFile:(NSString *)urlStr;

/**
 获取当前播放url的主链接(需要拼接的url)(已下载除外)

 @return 需要拼接的url
 */
+ (NSString *)getVideoOriginSpliceUrl:(NSString *)originUrl;

/**
 video resulotions

 @return 返回当前视频链接对应的分辨率字典
 */
- (NSDictionary *)getVideoResulotions;

/**
 分辨率下标

 @param width 视频presentationSize的宽度
 @return 分辨率下标
 */
- (NSInteger)getVideoPlayerResulotionTrackIndex:(CGFloat)width;

- (NSArray<NSString *> *)getSwithResolution:(int)trackIndex;

- (NSString *)getDownloadUrl:(int)trackIndex;

- (BOOL)containsDownloadUrl:(NSString *)url;

- (void)downloadSuccessAndDeleteDifferentResolutionCaches:(NSArray *)urls;

- (void)removeVideoAllCache;

@end

