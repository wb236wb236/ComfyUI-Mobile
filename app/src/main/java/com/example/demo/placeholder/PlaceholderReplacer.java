package com.example.demo.placeholder;

import android.util.Log;

import com.google.gson.JsonParser;

/**
 * 占位符替换器 — 移植自 st-chatu8 的 replacepro()
 * 在工作流 JSON 字符串中把 "%placeholder%" 替换为实际值
 */
public class PlaceholderReplacer {

    private static final String TAG = "PlaceholderReplacer";

    /**
     * 生成时的参数集合
     */
    public static class Payload {
        public String prompt = "";
        public String negativePrompt = "";
        public long seed = 0;
        public int steps = 0;
        public double cfgScale = 0;
        public int width = 0;
        public int height = 0;
        public String samplerName = "";
        public String scheduler = "";
        public String modelName = "";
        public String vae = "";
        public String clip = "";
        public String ipa = "";
        public double cQuanzhong = 0;
        public double cIdquanzhong = 0;
        public double cXijie = 0;
        public double cFenwei = 0;
        public String comfyuiCankaotupian = "";
        public String inpaintImage = "";
        public String inpaintMask = "";
        public double inpaintDenoise = 0.75;
        public String inpaintPositive = "";
        public String inpaintNegative = "";
    }

    /**
     * 检查工作流 JSON 中是否包含占位符
     */
    public static boolean hasPlaceholders(String workflowJson) {
        if (workflowJson == null) return false;
        for (PlaceholderMap.PlaceholderItem item : PlaceholderMap.MAP) {
            if (workflowJson.contains("\"" + item.placeholder + "\"")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将字符串值转为 JSON 字符串字面量（带引号）
     */
    private static String stringifyString(String value) {
        if (value == null) value = "";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }

    /**
     * 将数字值转为 JSON 字符串字面量（不带引号）
     */
    private static String stringifyNumber(double value, double fallback) {
        if (!Double.isFinite(value)) {
            return String.valueOf((long) fallback);
        }
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /**
     * 执行占位符替换
     * @param workflowJson 原始工作流 JSON 字符串
     * @param payload 参数值集合
     * @return 替换后的 JSON 字符串，如果替换后 JSON 无效则返回 null
     */
    public static String replace(String workflowJson, Payload payload) {
        if (workflowJson == null || payload == null) {
            return workflowJson;
        }

        String json = workflowJson;

        json = json.replace("\"%seed%\"", stringifyNumber(payload.seed, 0));
        json = json.replace("\"%steps%\"", stringifyNumber(payload.steps, 0));
        json = json.replace("\"%cfg_scale%\"", stringifyNumber(payload.cfgScale, 0));
        json = json.replace("\"%sampler_name%\"", stringifyString(payload.samplerName));
        json = json.replace("\"%width%\"", stringifyNumber(payload.width, 0));
        json = json.replace("\"%height%\"", stringifyNumber(payload.height, 0));
        json = json.replace("\"%negative_prompt%\"", stringifyString(payload.negativePrompt));
        json = json.replace("\"%prompt%\"", stringifyString(payload.prompt));
        json = json.replace("\"%MODEL_NAME%\"", stringifyString(payload.modelName));
        json = json.replace("\"%c_quanzhong%\"", stringifyNumber(payload.cQuanzhong, 0));
        json = json.replace("\"%c_idquanzhong%\"", stringifyNumber(payload.cIdquanzhong, 0));
        json = json.replace("\"%c_xijie%\"", stringifyNumber(payload.cXijie, 0));
        json = json.replace("\"%c_fenwei%\"", stringifyNumber(payload.cFenwei, 0));
        json = json.replace("\"%comfyuicankaotupian%\"", stringifyString(payload.comfyuiCankaotupian));
        json = json.replace("\"%ipa%\"", stringifyString(payload.ipa));
        json = json.replace("\"%scheduler%\"", stringifyString(payload.scheduler));
        json = json.replace("\"%vae%\"", stringifyString(payload.vae));
        json = json.replace("\"%clip%\"", stringifyString(payload.clip));

        if (payload.inpaintImage != null && !payload.inpaintImage.isEmpty()) {
            json = json.replace("\"%inpaint_image%\"", stringifyString(payload.inpaintImage));
        } else {
            json = json.replace("\"%inpaint_image%\"", stringifyString(""));
        }

        if (payload.inpaintMask != null && !payload.inpaintMask.isEmpty()) {
            json = json.replace("\"%inpaint_mask%\"", stringifyString(payload.inpaintMask));
        } else {
            json = json.replace("\"%inpaint_mask%\"", stringifyString(""));
        }

        json = json.replace("\"%inpaint_denoise%\"", stringifyNumber(payload.inpaintDenoise, 0.75));

        if (payload.inpaintPositive != null) {
            json = json.replace("\"%inpaint_positive%\"", stringifyString(payload.inpaintPositive));
        } else {
            json = json.replace("\"%inpaint_positive%\"", stringifyString(""));
        }

        if (payload.inpaintNegative != null) {
            json = json.replace("\"%inpaint_negative%\"", stringifyString(payload.inpaintNegative));
        } else {
            json = json.replace("\"%inpaint_negative%\"", stringifyString(""));
        }

        try {
            JsonParser.parseString(json).getAsJsonObject();
            return json;
        } catch (Exception e) {
            Log.e(TAG, "Placeholder replacement produced invalid JSON", e);
            return null;
        }
    }
}
