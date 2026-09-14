package com.example.demo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.manager.NodeConfigManager;
import com.example.demo.model.NodeConfig;
import com.example.demo.placeholder.PlaceholderReplacer;
import com.example.demo.view.AppHeader;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 节点配置管理界面 - 遵循原项目深色主题风格
 */
public class NodeManagerActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "comfy_node_prefs";
    private static final String KEY_PLACEHOLDER_MODE = "placeholder_mode_prefix_";

    private RecyclerView rvNodeConfigs;
    private LinearLayout nodeManagerRoot;
    private AppHeader appHeader;

    private NodeConfigManager configManager;
    private NodeConfigAdapter adapter;
    private List<NodeConfig> configList;

    private static final int REQUEST_IMPORT_FILE = 1001;
    private boolean isPasteMode = false;
    private String selectedFilePath = "";

    private EditText currentEtJsonContent;
    private String currentEditingConfigId;

    private final ActivityResultLauncher<Intent> visualEditorLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                String editedJson = result.getData().getStringExtra(WorkflowVisualEditorActivity.EXTRA_RESULT_JSON);
                if (editedJson != null && currentEtJsonContent != null) {
                    currentEtJsonContent.setText(editedJson);
                    Toast.makeText(this, "工作流已更新，请点击保存", Toast.LENGTH_SHORT).show();
                }
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 强制状态栏为深色背景，图标为浅色（适配深色 Header）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(R.color.comfy_bg_dark));
            // 清除 LIGHT_STATUS_BAR 标志，确保图标始终为白色
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        setContentView(R.layout.activity_node_manager);

        configManager = NodeConfigManager.getInstance(this);

        // 初始化 AppHeader 组件
        appHeader = findViewById(R.id.appHeader);
        
        // 设置标题
        appHeader.getTvHeaderBrand().setText("节点配置管理");
        
        // 显示返回箭头（替换 Logo 为向左箭头，同时隐藏右侧返回按钮）
        
        // 设置点击监听器
        appHeader.setOnHeaderClickListener(new AppHeader.OnHeaderClickListener() {
            @Override
            public void onBackClick() {
                finish();
            }

            @Override
            public void onNodeManagerClick() {
            }

            @Override
            public void onSettingsClick() {
            }

            @Override
            public void onHistoryClick() {
            }

            @Override
            public void onChatClick() {
                android.content.Intent intent = new android.content.Intent(NodeManagerActivity.this, ChatActivity.class);
                startActivity(intent);
            }

            @Override
            public void onReconnectClick() {
            }
        });

        // 初始化其他视图
        nodeManagerRoot = findViewById(R.id.nodeManagerRoot);
        rvNodeConfigs = findViewById(R.id.rvNodeConfigs);
        
        // 处理底部安全区域
        ViewCompat.setOnApplyWindowInsetsListener(nodeManagerRoot, (v, insets) -> {
            var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 底部安全区域处理
            int bottomMargin = systemBars.bottom > 0 ? systemBars.bottom : 0;
            if (rvNodeConfigs != null) {
                rvNodeConfigs.setPadding(
                    rvNodeConfigs.getPaddingLeft(),
                    rvNodeConfigs.getPaddingTop(),
                    rvNodeConfigs.getPaddingRight(),
                    bottomMargin + 20
                );
            }

            // 只消耗底部 insets，让顶部 insets 继续传递给 AppHeader
            return insets;
        });

        setupRecyclerView();
        loadConfigs();
    }

    private void setupRecyclerView() {
        configList = new ArrayList<>();
        adapter = new NodeConfigAdapter(configList, new NodeConfigAdapter.OnConfigClickListener() {
            @Override
            public void onConfigClick(NodeConfig config) {
                showConfigOptionsDialog(config);
            }

            @Override
            public void onAddNodeClick() {
                showAddDialog();
            }
        });
        rvNodeConfigs.setLayoutManager(new LinearLayoutManager(this));
        rvNodeConfigs.setAdapter(adapter);
    }

    private void loadConfigs() {
        configList.clear();
        configList.addAll(configManager.loadAllConfigs());
        adapter.notifyDataSetChanged();
    }

    /**
     * 显示添加配置对话框
     */
    private void showAddDialog() {
        isPasteMode = false;
        selectedFilePath = "";

        View dialogView = View.inflate(this, R.layout.dialog_node_config_editor, null);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        TextView rbFromFile = dialogView.findViewById(R.id.rbFromFile);
        TextView rbFromPaste = dialogView.findViewById(R.id.rbFromPaste);
        TextView btnSelectFile = dialogView.findViewById(R.id.btnSelectFile);
        TextView tvSelectedFile = dialogView.findViewById(R.id.tvSelectedFile);
        EditText etJsonContent = dialogView.findViewById(R.id.etJsonContent);
        TextView btnVisualEditor = dialogView.findViewById(R.id.btnVisualEditor);
        SwitchCompat swPlaceholderMode = dialogView.findViewById(R.id.swPlaceholderMode);

        currentEtJsonContent = etJsonContent;
        currentEditingConfigId = null;

        swPlaceholderMode.setChecked(false);

        // 默认显示文件选择
        updateUploadModeUI(rbFromFile, rbFromPaste, btnSelectFile, etJsonContent);

        rbFromFile.setOnClickListener(v -> {
            isPasteMode = false;
            updateUploadModeUI(rbFromFile, rbFromPaste, btnSelectFile, etJsonContent);
        });

        rbFromPaste.setOnClickListener(v -> {
            isPasteMode = true;
            updateUploadModeUI(rbFromPaste, rbFromFile, btnSelectFile, etJsonContent);
        });

        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/json");
            startActivityForResult(intent, REQUEST_IMPORT_FILE);
        });

        btnVisualEditor.setOnClickListener(v -> {
            String jsonContent = getDialogContent(isPasteMode, etJsonContent);
            if (jsonContent == null || jsonContent.isEmpty()) {
                Toast.makeText(this, "请先输入或粘贴工作流 JSON", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, WorkflowVisualEditorActivity.class);
            intent.putExtra(WorkflowVisualEditorActivity.EXTRA_WORKFLOW_JSON, jsonContent);
            visualEditorLauncher.launch(intent);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setOnShowListener(d -> {
            Button posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (posBtn != null) {
                posBtn.setTextColor(Color.parseColor("#DFFF00"));
                posBtn.setAllCaps(false);
            }
            if (negBtn != null) {
                negBtn.setTextColor(Color.parseColor("#AAAAAA"));
                negBtn.setAllCaps(false);
            }
        });

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "创建", (d, which) -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "请输入配置名称", Toast.LENGTH_SHORT).show();
                return;
            }

            String jsonContent;
            if (isPasteMode) {
                jsonContent = etJsonContent.getText().toString().trim();
                if (jsonContent.isEmpty()) {
                    Toast.makeText(this, "JSON 内容不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                if (selectedFilePath.isEmpty()) {
                    Toast.makeText(this, "请选择 JSON 文件", Toast.LENGTH_SHORT).show();
                    return;
                }
                jsonContent = selectedFilePath;
            }

            NodeConfig config = configManager.createConfig(name,
                etDescription.getText().toString().trim(), jsonContent);
            if (config != null) {
                setPlaceholderMode(config.getId(), swPlaceholderMode.isChecked());
                loadConfigs();
                Toast.makeText(this, "配置创建成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "配置创建失败，请检查 JSON 格式", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "取消", (d, which) -> d.dismiss());

        dialog.show();
    }

    private void updateUploadModeUI(TextView active, TextView inactive, 
                                     TextView btnSelectFile, EditText etJsonContent) {
        active.setBackgroundResource(R.drawable.comfy_preset_selected_bg);
        active.setTextColor(0xFF000000);
        active.setAlpha(1.0f);
        active.setTypeface(null, android.graphics.Typeface.BOLD);
        
        inactive.setBackgroundResource(R.drawable.comfy_action_btn_bg);
        inactive.setTextColor(0xFF777777);
        inactive.setAlpha(1.0f);
        inactive.setTypeface(null, android.graphics.Typeface.NORMAL);
        
        if (active.getText().toString().equals("从文件")) {
            btnSelectFile.setVisibility(View.VISIBLE);
            etJsonContent.setVisibility(View.GONE);
        } else {
            btnSelectFile.setVisibility(View.GONE);
            etJsonContent.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 显示配置操作选项对话框
     */
    private void showConfigOptionsDialog(NodeConfig config) {
        final String[] options;
        if (config.getType() == NodeConfig.ConfigType.BUILTIN) {
            options = new String[]{"设为激活", "查看详情"};
        } else {
            options = new String[]{"设为激活", "编辑", "删除", "查看详情"};
        }
        final int detailIndex = options.length - 1; // "查看详情" 始终是最后一个
        final boolean isCustom = config.getType() != NodeConfig.ConfigType.BUILTIN;

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_Demo_ConfigDialog)
                .setTitle(config.getName())
                .setAdapter(new ArrayAdapter<String>(this, R.layout.item_dialog_option, R.id.list_item_1, options) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = convertView != null ? convertView :
                                LayoutInflater.from(getContext()).inflate(R.layout.item_dialog_option, parent, false);
                        TextView tv = view.findViewById(R.id.list_item_1);
                        tv.setText(getItem(position));
                        tv.setTextColor(Color.parseColor("#e0e0e0")); // 全部白色
                        return view;
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            configManager.setActiveConfig(config.getId());
                            loadConfigs();
                            Toast.makeText(NodeManagerActivity.this, "已切换至：" + config.getName(), Toast.LENGTH_SHORT).show();
                        } else if (which == detailIndex) {
                            showConfigDetailDialog(config);
                        } else if (isCustom && which == 1) {
                            showEditDialog(config);
                        } else if (isCustom && which == 2) {
                            showDeleteConfirmDialog(config);
                        }
                    }
                })
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.comfy_dialog_bg);
        }

        dialog.show();
    }

    /**
     * 显示编辑对话框
     */
    private void showEditDialog(NodeConfig config) {
        View dialogView = View.inflate(this, R.layout.dialog_node_config_editor, null);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        EditText etJsonContent = dialogView.findViewById(R.id.etJsonContent);
        TextView rbFromFile = dialogView.findViewById(R.id.rbFromFile);
        TextView rbFromPaste = dialogView.findViewById(R.id.rbFromPaste);
        TextView btnSelectFile = dialogView.findViewById(R.id.btnSelectFile);
        TextView btnVisualEditor = dialogView.findViewById(R.id.btnVisualEditor);
        SwitchCompat swPlaceholderMode = dialogView.findViewById(R.id.swPlaceholderMode);

        currentEtJsonContent = etJsonContent;
        currentEditingConfigId = config.getId();

        // 预填数据
        etName.setText(config.getName());
        etDescription.setText(config.getDescription() != null ? config.getDescription() : "");

        // 默认使用粘贴模式显示 JSON
        isPasteMode = true;
        updateUploadModeUI(rbFromPaste, rbFromFile, btnSelectFile, etJsonContent);

        String currentContent = configManager.getConfigJsonContent(config.getId());
        if (currentContent != null) {
            etJsonContent.setText(currentContent);
        }

        // 读取占位符开关状态
        swPlaceholderMode.setChecked(getPlaceholderMode(config.getId()));

        rbFromFile.setOnClickListener(v -> {
            isPasteMode = false;
            updateUploadModeUI(rbFromFile, rbFromPaste, btnSelectFile, etJsonContent);
        });

        rbFromPaste.setOnClickListener(v -> {
            isPasteMode = true;
            updateUploadModeUI(rbFromPaste, rbFromFile, btnSelectFile, etJsonContent);
        });

        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/json");
            startActivityForResult(intent, REQUEST_IMPORT_FILE);
        });

        btnVisualEditor.setOnClickListener(v -> {
            String jsonContent = getDialogContent(isPasteMode, etJsonContent);
            if (jsonContent == null || jsonContent.isEmpty()) {
                Toast.makeText(this, "请先输入或粘贴工作流 JSON", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, WorkflowVisualEditorActivity.class);
            intent.putExtra(WorkflowVisualEditorActivity.EXTRA_WORKFLOW_JSON, jsonContent);
            visualEditorLauncher.launch(intent);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setOnShowListener(d -> {
            Button posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (posBtn != null) {
                posBtn.setTextColor(Color.parseColor("#DFFF00"));
                posBtn.setAllCaps(false);
            }
            if (negBtn != null) {
                negBtn.setTextColor(Color.parseColor("#AAAAAA"));
                negBtn.setAllCaps(false);
            }
        });

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "保存", (d, which) -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "请输入配置名称", Toast.LENGTH_SHORT).show();
                return;
            }

            String jsonContent;
            if (isPasteMode) {
                jsonContent = etJsonContent.getText().toString().trim();
                if (jsonContent.isEmpty()) {
                    Toast.makeText(this, "JSON 内容不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                if (selectedFilePath.isEmpty()) {
                    Toast.makeText(this, "请选择 JSON 文件", Toast.LENGTH_SHORT).show();
                    return;
                }
                jsonContent = selectedFilePath;
            }

            boolean success = configManager.updateConfig(config.getId(), name,
                etDescription.getText().toString().trim(), jsonContent);
            if (success) {
                setPlaceholderMode(config.getId(), swPlaceholderMode.isChecked());
                loadConfigs();
                Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "保存失败，请检查 JSON 格式", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "取消", (d, which) -> d.dismiss());

        dialog.show();
    }

    /**
     * 显示详情对话框
     */
    private void showConfigDetailDialog(NodeConfig config) {
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        TextView tvDetail = new TextView(this);
        tvDetail.setPadding(40, 40, 40, 40);
        tvDetail.setTextColor(0xFFCCCCCC);
        tvDetail.setTextSize(13);
        scrollView.addView(tvDetail);

        StringBuilder sb = new StringBuilder();
        sb.append("名称：").append(config.getName()).append("\n\n");
        sb.append("描述：").append(config.getDescription() != null ? config.getDescription() : "无").append("\n\n");
        sb.append("类型：").append(config.getType() == NodeConfig.ConfigType.BUILTIN ? "内置" : "自定义").append("\n\n");
        sb.append("创建时间：").append(formatDate(config.getCreatedAt())).append("\n");
        sb.append("修改时间：").append(formatDate(config.getModifiedAt())).append("\n");

        String content = configManager.getConfigJsonContent(config.getId());
        if (content != null) {
            sb.append("\nJSON 内容预览:\n").append(previewJson(content, 500));
        }

        tvDetail.setText(sb.toString());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("配置详情")
                .setView(scrollView)
                .setPositiveButton("关闭", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.comfy_dialog_bg);
        }
        
        dialog.setOnShowListener(d -> {
            Button posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (posBtn != null) {
                posBtn.setTextColor(Color.parseColor("#DFFF00"));
            }
        });

        dialog.show();
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(NodeConfig config) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除配置 \"" + config.getName() + "\" 吗？")
                .setPositiveButton("删除", (d, which) -> {
                    boolean success = configManager.deleteConfig(config.getId());
                    if (success) {
                        loadConfigs();
                        Toast.makeText(this, "配置已删除", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.comfy_dialog_bg);
        }

        dialog.setOnShowListener(d -> {
            Button posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (posBtn != null) {
                posBtn.setTextColor(Color.parseColor("#FF4D4D"));
            }
            if (negBtn != null) {
                negBtn.setTextColor(Color.parseColor("#AAAAAA"));
            }
        });

        dialog.show();
    }

    /**
     * 格式化日期
     */
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * 预览 JSON 内容（截断）
     */
    private String previewJson(String json, int maxLength) {
        if (json.length() <= maxLength) {
            return json;
        }
        return json.substring(0, maxLength) + "...";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMPORT_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                    selectedFilePath = new String(buffer, "UTF-8");

                    Toast.makeText(this, "文件已选择", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "读取文件失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 获取对话框中的 JSON 内容
     */
    private String getDialogContent(boolean pasteMode, EditText etJsonContent) {
        if (pasteMode) {
            return etJsonContent.getText().toString().trim();
        } else {
            return selectedFilePath;
        }
    }

    /**
     * 保存占位符模式开关状态
     */
    private void setPlaceholderMode(String configId, boolean enabled) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_PLACEHOLDER_MODE + configId, enabled).apply();
    }

    /**
     * 读取占位符模式开关状态
     */
    private boolean getPlaceholderMode(String configId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_PLACEHOLDER_MODE + configId, false);
    }

    /**
     * 静态方法：读取指定配置的占位符模式
     */
    public static boolean isPlaceholderMode(android.content.Context context, String configId) {
        if (configId == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_PLACEHOLDER_MODE + configId, false);
    }
}
