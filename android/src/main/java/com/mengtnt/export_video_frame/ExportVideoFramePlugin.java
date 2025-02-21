package com.mengtnt.export_video_frame;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class ExportVideoFramePlugin implements FlutterPlugin, MethodCallHandler {
  private MethodChannel channel;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "export_video_frame");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
    channel = null;
  }

  @Override
  public void onMethodCall(MethodCall call, final Result result) {
    if (!PermissionManager.current().isPermissionGranted()) {
      PermissionManager.current().askForPermission();
    }
    if (!(FileStorage.isExternalStorageReadable() && FileStorage.isExternalStorageWritable())) {
      result.error("File permission exception", "Not get external storage permission", null);
      return;
    }

    switch (call.method) {
      case "cleanImageCache": {
        Boolean success = FileStorage.share().cleanCache();
        if (success) {
          result.success("success");
        } else {
          result.error("Clean exception", "Fail", null);
        }
        break;
      }
      case "saveImage": {
        String filePath = call.argument("filePath");
        String albumName = call.argument("albumName");
        Bitmap waterBitMap = null;
        PointF waterPoint = null;
        Double scale = 1.0;
        AblumSaver.share().setAlbumName(albumName);
        if (call.argument("waterMark") != null && call.argument("alignment") != null) {
          String waterPathKey = call.argument("waterMark");
          AssetManager assetManager = flutterPluginBinding.getApplicationContext().getAssets();
          String key = call.argument("waterMark");
          Map<String, Number> rect = call.argument("alignment");
          Double x = rect.get("x").doubleValue();
          Double y = rect.get("y").doubleValue();
          waterPoint = new PointF(x.floatValue(), y.floatValue());
          Number number = call.argument("scale");
          scale = number.doubleValue();
          try {
            InputStream in = assetManager.open(key);
            waterBitMap = BitmapFactory.decodeStream(in);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        AblumSaver.share().saveToAlbum(filePath, waterBitMap, waterPoint, scale.floatValue(), result);
        break;
      }
      case "exportGifImagePathList": {
        String filePath = call.argument("filePath");
        Number quality = call.argument("quality");
        ExportImageTask task = new ExportImageTask();
        task.execute(filePath, quality);
        task.setCallBack(new Callback() {
          @Override
          public void exportPath(ArrayList<String> list) {
            if (list != null) {
              result.success(list);
            } else {
              result.error("Media exception", "Get frame fail", null);
            }
          }
        });
        break;
      }
      case "exportImage": {
        String filePath = call.argument("filePath");
        Number number = call.argument("number");
        Number quality = call.argument("quality");
        ExportImageTask task = new ExportImageTask();
        task.execute(filePath, number.intValue(), quality);
        task.setCallBack(new Callback() {
          @Override
          public void exportPath(ArrayList<String> list) {
            if (list != null) {
              result.success(list);
            } else {
              result.error("Media exception", "Get frame fail", null);
            }
          }
        });
        break;
      }
      case "exportImageBySeconds": {
        String filePath = call.argument("filePath");
        Number duration = call.argument("duration");
        Number radian = call.argument("radian");
        ExportImageTask task = new ExportImageTask();
        task.execute(filePath, duration.longValue(), radian);
        task.setCallBack(new Callback() {
          @Override
          public void exportPath(ArrayList<String> list) {
            if ((list != null) && (list.size() > 0)) {
              result.success(list.get(0));
            } else {
              result.error("Media exception", "Get frame fail", null);
            }
          }
        });
        break;
      }
      default:
        result.notImplemented();
        break;
    }
  }
}