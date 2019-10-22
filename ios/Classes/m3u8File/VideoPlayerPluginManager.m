//
//  VideoPlayerPluginManager.m
//  video_player
//
//  Created by 牛新怀 on 2019/10/21.
//

#import "VideoPlayerPluginManager.h"

static NSString * const spltSlash = @"/";

@implementation VideoPlayerPluginManager

- (instancetype)initWithOriginPlayerUrl:(NSString *)url {
    if (self = [super init]) {
        if ([url containsString:spltSlash]) {
            self.spliceOriginUrl = [VideoPlayerPluginManager getVideoOriginSpliceUrl:url];
        }
        NSDictionary * urlDic = [VideoPlayerPluginManager getM3U8AllFile:url];
        if (![urlDic isKindOfClass:[NSNull class]] && urlDic.count != 0) {
            self.resolutionArray = [urlDic allKeys];
            NSArray * values = [urlDic allValues];
            NSMutableArray * spliceArray = [[NSMutableArray alloc] init];
            if (values.count != 0) {
                for (NSString * value in values) {
                    if (values != nil && ![value isEqualToString:@""]) {
                        NSString * spliceUrl = [self.spliceOriginUrl stringByAppendingString:value];
                        [spliceArray addObject:spliceUrl];
                    }
                }
                if (spliceArray.count != 0) {
                    self.resolutionDownloadUrlArray = [NSArray arrayWithArray:spliceArray];
                }
            }
        }
    }
    return self;
}

- (NSDictionary *)getVideoResulotions {
    NSMutableDictionary * dic = [[NSMutableDictionary alloc] init];
    if (self.resolutionArray != nil && self.resolutionArray.count != 0) {
        for (int i = 0; i < self.resolutionArray.count; ++i) {
            NSString * resolutionString = self.resolutionArray[i];
            if (resolutionString != nil) {
                [dic setObject:resolutionString forKey:[NSNumber numberWithInt:i]];
            }
        }
    }
    return dic;
}

- (NSInteger)getVideoPlayerResulotionTrackIndex:(CGFloat)width {
    NSInteger trackIndex = 0;
    if (self.resolutionArray != nil && self.resolutionArray.count != 0) {
        for (int i = 0; i < self.resolutionArray.count; ++i) {
            NSString * resolutionString = self.resolutionArray[i];
            if (resolutionString != nil) {
                if ([resolutionString containsString: [NSString stringWithFormat:@"%f",width]]) {
                    trackIndex = i;
                    break;
                }
            }
        }
    }
    return trackIndex;
}

+ (NSString *)getVideoOriginSpliceUrl:(NSString *)originUrl {
    NSString * spltStr = [originUrl componentsSeparatedByString:@"/"].lastObject;
    NSString * origin = [originUrl stringByReplacingOccurrencesOfString:spltStr withString:@""];
    return origin;
}

- (NSArray<NSString *> *)getSwithResolution:(int)trackIndex {
    NSMutableArray * array = [[NSMutableArray alloc] init];
    if (self.resolutionArray != nil && self.resolutionArray.count != 0) {
        for (int i = 0; i < self.resolutionArray.count; ++i) {
            NSString * resolutionString = self.resolutionArray[i];
            if (i == trackIndex) {
                if (resolutionString != nil) {
                    if ([resolutionString containsString:@"x"]) {
                        [array addObjectsFromArray:[resolutionString componentsSeparatedByString:@"x"]];
                        break;
                    }
                }
            }
        }
    }
    return array;
}

+ (NSDictionary *)getM3U8AllFile:(NSString *)urlStr {
    NSMutableDictionary * dic = [[NSMutableDictionary alloc] init];
    NSString * spltOne = @",";
    NSString * spltTwo = @"x";
    NSString * spltThree = @"=";
    NSString *oriM3u8String = [NSString stringWithContentsOfURL:[NSURL URLWithString:urlStr] encoding:0 error:nil];;
    NSArray * array = [oriM3u8String componentsSeparatedByString:@"\n"];
    NSString * suffixString = @".m3u8";
    NSString * resolationString = @"";
    for (NSString * comString in array) {
        if ([comString hasSuffix:suffixString]) {
            if (![resolationString isEqualToString:@""]) {
                [dic setObject:comString forKey:resolationString];
            }
        } else if (![comString isEqualToString:@""]) {
            resolationString = @"";
            if ([comString containsString:spltOne]) {
                NSArray * resolationArray = [comString componentsSeparatedByString:spltOne];
                if (resolationArray.count != 0) {
                    for (NSString * oneStr in resolationArray) {
                        if ([oneStr containsString:spltThree]) {
                            NSArray * threeArray = [oneStr componentsSeparatedByString:spltThree];
                            if (threeArray.count != 0) {
                                for (NSString * threeStr in threeArray) {
                                    if ([threeStr containsString:spltTwo]) {
                                        resolationString = threeStr;
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    return dic;
}

- (void)setSpliceOriginUrl:(NSString *)spliceOriginUrl {
    _spliceOriginUrl = spliceOriginUrl;
}

- (void)setResolutionArray:(NSArray *)resolutionArray {
    _resolutionArray = resolutionArray;
}

- (void)setResolutionDownloadUrlArray:(NSArray *)resolutionDownloadUrlArray {
    _resolutionDownloadUrlArray = resolutionDownloadUrlArray;
}

@end
