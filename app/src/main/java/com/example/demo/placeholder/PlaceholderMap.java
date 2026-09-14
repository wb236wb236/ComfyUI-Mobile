package com.example.demo.placeholder;

import java.util.ArrayList;
import java.util.List;

/**
 * 占位符定义表 — 移植自 st-chatu8 的 PLACEHOLDER_MAP
 * 每个占位符对应工作流 JSON 中的一个可替换字段
 */
public class PlaceholderMap {

    public static class PlaceholderItem {
        public final String placeholder;
        public final String label;
        public final String[] matchKeys;
        public final String type;

        public PlaceholderItem(String placeholder, String label, String[] matchKeys, String type) {
            this.placeholder = placeholder;
            this.label = label;
            this.matchKeys = matchKeys;
            this.type = type;
        }
    }

    public static final List<PlaceholderItem> MAP = new ArrayList<>();

    static {
        MAP.add(new PlaceholderItem("%seed%", "种子 (seed)", new String[]{"seed"}, "number"));
        MAP.add(new PlaceholderItem("%steps%", "步数 (steps)", new String[]{"steps"}, "number"));
        MAP.add(new PlaceholderItem("%cfg_scale%", "CFG (cfg)", new String[]{"cfg", "cfg_scale"}, "number"));
        MAP.add(new PlaceholderItem("%sampler_name%", "采样器 (sampler)", new String[]{"sampler_name", "sampler"}, "string"));
        MAP.add(new PlaceholderItem("%scheduler%", "调度器 (scheduler)", new String[]{"scheduler"}, "string"));
        MAP.add(new PlaceholderItem("%width%", "宽度 (width)", new String[]{"width"}, "number"));
        MAP.add(new PlaceholderItem("%height%", "高度 (height)", new String[]{"height"}, "number"));
        MAP.add(new PlaceholderItem("%prompt%", "正面提示词 (prompt)", new String[]{"positive", "text"}, "string"));
        MAP.add(new PlaceholderItem("%negative_prompt%", "负面提示词 (negative)", new String[]{"negative"}, "string"));
        MAP.add(new PlaceholderItem("%MODEL_NAME%", "模型 (ckpt_name)", new String[]{"ckpt_name"}, "string"));
        MAP.add(new PlaceholderItem("%vae%", "VAE", new String[]{"vae_name", "vae"}, "string"));
        MAP.add(new PlaceholderItem("%clip%", "CLIP", new String[]{"clip_name"}, "string"));
        MAP.add(new PlaceholderItem("%c_quanzhong%", "IPA权重", new String[]{"c_quanzhong"}, "number"));
        MAP.add(new PlaceholderItem("%c_idquanzhong%", "FaceID权重", new String[]{"c_idquanzhong"}, "number"));
        MAP.add(new PlaceholderItem("%c_xijie%", "细节强度", new String[]{"c_xijie"}, "number"));
        MAP.add(new PlaceholderItem("%c_fenwei%", "氛围强度", new String[]{"c_fenwei"}, "number"));
        MAP.add(new PlaceholderItem("%comfyuicankaotupian%", "参考图", new String[]{"comfyuicankaotupian", "image"}, "string"));
        MAP.add(new PlaceholderItem("%ipa%", "IPA类型", new String[]{"ipa"}, "string"));
        MAP.add(new PlaceholderItem("%inpaint_image%", "重绘原图", new String[]{"inpaint_image"}, "string"));
        MAP.add(new PlaceholderItem("%inpaint_mask%", "重绘遮罩", new String[]{"inpaint_mask"}, "string"));
        MAP.add(new PlaceholderItem("%inpaint_denoise%", "重绘强度", new String[]{"inpaint_denoise", "denoise"}, "number"));
        MAP.add(new PlaceholderItem("%inpaint_positive%", "重绘正面提示词", new String[]{"inpaint_positive"}, "string"));
        MAP.add(new PlaceholderItem("%inpaint_negative%", "重绘负面提示词", new String[]{"inpaint_negative"}, "string"));
    }

    /**
     * 根据输入名推荐占位符
     */
    public static String getRecommendedPlaceholder(String inputName) {
        if (inputName == null) return null;
        String lowerName = inputName.toLowerCase();
        for (PlaceholderItem item : MAP) {
            for (String key : item.matchKeys) {
                if (lowerName.equals(key.toLowerCase()) || lowerName.contains(key.toLowerCase())) {
                    return item.placeholder;
                }
            }
        }
        return null;
    }

    /**
     * 判断值是否为占位符
     */
    public static boolean isPlaceholderValue(String value) {
        if (value == null || value.length() < 2) return false;
        return value.startsWith("%") && value.endsWith("%");
    }

    /**
     * 获取所有占位符列表（用于 WebView 注入）
     */
    public static String toJsonArrayString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < MAP.size(); i++) {
            PlaceholderItem item = MAP.get(i);
            sb.append("{");
            sb.append("\"placeholder\":\"").append(item.placeholder).append("\",");
            sb.append("\"label\":\"").append(item.label).append("\",");
            sb.append("\"type\":\"").append(item.type).append("\"");
            sb.append("}");
            if (i < MAP.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
