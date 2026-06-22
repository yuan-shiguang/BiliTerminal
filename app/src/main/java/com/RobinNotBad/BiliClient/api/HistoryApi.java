package com.RobinNotBad.BiliClient.api;

import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.ApiResult;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class HistoryApi {

    public static class HistoryData {
        @SerializedName("list")
        public List<HistoryItem> list;
        @SerializedName("cursor")
        public CursorData cursor;
    }

    public static class HistoryItem {
        @SerializedName("title")
        public String title;
        @SerializedName("cover")
        public String cover;
        @SerializedName("author_name")
        public String author_name;
        @SerializedName("progress")
        public int progress;
        @SerializedName("history")
        public HistoryRef history;
    }

    public static class HistoryRef {
        @SerializedName("oid")
        public long oid;
        @SerializedName("bvid")
        public String bvid;
    }

    public static class CursorData {
        @SerializedName("business")
        public String business;
        @SerializedName("max")
        public long max;
        @SerializedName("view_at")
        public long view_at;
    }

    public static void reportHistory(long aid, long cid, long progress) throws IOException {
        if (SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.PRIVACY_MODE, false)) {
            return;
        }
        String url = "https://api.bilibili.com/x/v2/history/report";
        String per = "aid=" + aid + "&cid=" + cid
                + "&progress=" + (progress >= 0 ? progress : "")
                + "&platform=pc"
                + "&csrf=" + SharedPreferencesUtil.getString(SharedPreferencesUtil.csrf, "");
        NetWorkUtil.post(url, per, NetWorkUtil.webHeaders);
    }

    public static ApiResult getHistory(ApiResult lastResult, List<VideoCard> videoList) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/web-interface/history/cursor?type=archive&view_at=" + lastResult.timestamp + "&business=" + lastResult.business + "&max=" + lastResult.offset;
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<HistoryData> resp = GsonUtil.fromJson(json,
                new com.google.gson.reflect.TypeToken<ApiResponse<HistoryData>>(){}.getType());

        ApiResult apiResult = new ApiResult();
        if (resp != null) {
            apiResult.code = resp.code;
            apiResult.message = resp.message;
        }

        if (resp == null || !resp.isSuccess() || resp.data == null) return apiResult;

        if (resp.data.list != null) {
            for (HistoryItem item : resp.data.list) {
                if (item == null) continue;
                String viewStr = item.progress == 0 ? "还没看过" : "看到" + StringUtil.toTime(item.progress);
                long aid = item.history != null ? item.history.oid : 0;
                String bvid = item.history != null ? item.history.bvid : "";
                videoList.add(new VideoCard(item.title, item.author_name, viewStr, item.cover, aid, bvid));
            }
            if (resp.data.list.isEmpty()) apiResult.isBottom = true;
        }

        if (resp.data.cursor != null) {
            apiResult.business = resp.data.cursor.business != null ? resp.data.cursor.business : "";
            apiResult.offset = resp.data.cursor.max;
            apiResult.timestamp = resp.data.cursor.view_at;
        }

        return apiResult;
    }
}
