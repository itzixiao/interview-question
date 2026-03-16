package cn.itzixiao.interview.lowcode.form;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 动态表单引擎 - 基于 JSON Schema 的表单渲染
 * <p>
 * 核心功能：
 * 1. 表单定义（JSON Schema）
 * 2. 动态渲染
 * 3. 数据绑定
 * 4. 表单验证
 * <p>
 * 支持的组件类型：
 * - input: 输入框
 * - textarea: 文本域
 * - select: 下拉选择
 * - radio: 单选框
 * - checkbox: 复选框
 * - date: 日期选择器
 * - number: 数字输入
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class DynamicFormEngine {

    /**
     * 渲染表单
     *
     * @param formDefinition 表单定义
     * @return 渲染后的 HTML
     */
    public String renderForm(FormDefinition formDefinition) {
        log.info("========== 渲染动态表单 ==========");
        log.info("表单标题：{}", formDefinition.getTitle());
        log.info("表单描述：{}", formDefinition.getDescription());
        log.info("字段数量：{}", formDefinition.getFields().size());

        StringBuilder html = new StringBuilder();
        html.append("<form class=\"dynamic-form\">\n");
        html.append("  <h2>").append(formDefinition.getTitle()).append("</h2>\n");
        html.append("  <p>").append(formDefinition.getDescription()).append("</p>\n");

        for (FormField field : formDefinition.getFields()) {
            html.append(renderField(field));
        }

        html.append("  <button type=\"submit\">提交</button>\n");
        html.append("</form>");

        log.info("\n生成的 HTML:\n{}", html.toString());
        log.info("==============================\n");

        return html.toString();
    }

    /**
     * 渲染单个字段
     */
    private String renderField(FormField field) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <div class=\"form-field\">\n");
        sb.append("    <label>").append(field.getLabel());

        if (field.isRequired()) {
            sb.append(" <span class=\"required\">*</span>");
        }
        sb.append("</label>\n");

        switch (field.getType()) {
            case "input":
                sb.append("    <input type=\"text\" name=\"").append(field.getName())
                        .append("\" placeholder=\"").append(field.getPlaceholder()).append("\"");
                if (field.isRequired()) sb.append(" required");
                sb.append("/>\n");
                break;

            case "textarea":
                sb.append("    <textarea name=\"").append(field.getName())
                        .append("\" placeholder=\"").append(field.getPlaceholder()).append("\"");
                if (field.isRequired()) sb.append(" required");
                sb.append("></textarea>\n");
                break;

            case "select":
                sb.append("    <select name=\"").append(field.getName()).append("\">\n");
                sb.append("      <option value=\"\">请选择</option>\n");
                for (Map.Entry<String, String> option : field.getOptions().entrySet()) {
                    sb.append("      <option value=\"").append(option.getKey())
                            .append("\">").append(option.getValue()).append("</option>\n");
                }
                sb.append("    </select>\n");
                break;

            case "radio":
                for (Map.Entry<String, String> option : field.getOptions().entrySet()) {
                    sb.append("    <input type=\"radio\" name=\"").append(field.getName())
                            .append("\" value=\"").append(option.getKey()).append("\" id=\"")
                            .append(field.getName()).append("_").append(option.getKey()).append("\">\n");
                    sb.append("    <label for=\"").append(field.getName()).append("_")
                            .append(option.getKey()).append("\">").append(option.getValue()).append("</label>\n");
                }
                break;

            case "checkbox":
                for (Map.Entry<String, String> option : field.getOptions().entrySet()) {
                    sb.append("    <input type=\"checkbox\" name=\"").append(field.getName())
                            .append("[]\" value=\"").append(option.getKey()).append("\" id=\"")
                            .append(field.getName()).append("_").append(option.getKey()).append("\">\n");
                    sb.append("    <label for=\"").append(field.getName()).append("_")
                            .append(option.getKey()).append("\">").append(option.getValue()).append("</label>\n");
                }
                break;

            case "date":
                sb.append("    <input type=\"date\" name=\"").append(field.getName()).append("\"");
                if (field.isRequired()) sb.append(" required");
                sb.append("/>\n");
                break;

            case "number":
                sb.append("    <input type=\"number\" name=\"").append(field.getName())
                        .append("\" step=\"").append(field.getStep() != null ? field.getStep() : "1").append("\"");
                if (field.getMin() != null) sb.append(" min=\"").append(field.getMin()).append("\"");
                if (field.getMax() != null) sb.append(" max=\"").append(field.getMax()).append("\"");
                if (field.isRequired()) sb.append(" required");
                sb.append("/>\n");
                break;
        }

        sb.append("  </div>\n");
        return sb.toString();
    }

    /**
     * 验证表单数据
     */
    public ValidationResult validate(FormDefinition formDefinition, Map<String, Object> formData) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        result.setErrors(new HashMap<>());

        for (FormField field : formDefinition.getFields()) {
            String fieldName = field.getName();
            Object value = formData.get(fieldName);

            // 必填验证
            if (field.isRequired() && (value == null || "".equals(value.toString().trim()))) {
                result.getErrors().put(fieldName, field.getLabel() + " 不能为空");
                result.setValid(false);
            }

            // 长度验证
            if (value != null && field.getMaxLength() != null) {
                if (value.toString().length() > field.getMaxLength()) {
                    result.getErrors().put(fieldName, field.getLabel() + " 长度不能超过 " + field.getMaxLength());
                    result.setValid(false);
                }
            }

            // 范围验证
            if (value instanceof Number && field.getMin() != null && field.getMax() != null) {
                double numValue = ((Number) value).doubleValue();
                if (numValue < field.getMin() || numValue > field.getMax()) {
                    result.getErrors().put(fieldName, field.getLabel() + " 必须在 " + field.getMin() + " 到 " + field.getMax() + " 之间");
                    result.setValid(false);
                }
            }
        }

        return result;
    }

    /**
     * 演示表单生成
     */
    public void demoFormGeneration() {
        log.info("\n============================================================");
        log.info("低代码平台 - 动态表单引擎演示");
        log.info("============================================================\n");

        // 创建用户注册表单
        FormDefinition userForm = new FormDefinition();
        userForm.setTitle("用户注册表单");
        userForm.setDescription("请填写用户基本信息");

        List<FormField> fields = new ArrayList<>();

        // 用户名输入框
        FormField username = new FormField();
        username.setName("username");
        username.setType("input");
        username.setLabel("用户名");
        username.setPlaceholder("请输入用户名");
        username.setRequired(true);
        username.setMaxLength(20);
        fields.add(username);

        // 邮箱输入框
        FormField email = new FormField();
        email.setName("email");
        email.setType("input");
        email.setLabel("邮箱");
        email.setPlaceholder("请输入邮箱地址");
        email.setRequired(true);
        fields.add(email);

        // 性别单选框
        FormField gender = new FormField();
        gender.setName("gender");
        gender.setType("radio");
        gender.setLabel("性别");
        gender.setRequired(true);
        gender.setOptions(new LinkedHashMap<String, String>() {{
            put("male", "男");
            put("female", "女");
        }});
        fields.add(gender);

        // 爱好复选框
        FormField hobby = new FormField();
        hobby.setName("hobby");
        hobby.setType("checkbox");
        hobby.setLabel("爱好");
        hobby.setOptions(new LinkedHashMap<String, String>() {{
            put("reading", "阅读");
            put("music", "音乐");
            put("sports", "运动");
            put("travel", "旅行");
        }});
        fields.add(hobby);

        // 学历下拉选择
        FormField education = new FormField();
        education.setName("education");
        education.setType("select");
        education.setLabel("学历");
        education.setRequired(true);
        education.setOptions(new LinkedHashMap<String, String>() {{
            put("high_school", "高中");
            put("bachelor", "本科");
            put("master", "硕士");
            put("doctor", "博士");
        }});
        fields.add(education);

        // 年龄数字输入
        FormField age = new FormField();
        age.setName("age");
        age.setType("number");
        age.setLabel("年龄");
        age.setMin(18.0);
        age.setMax(100.0);
        age.setRequired(true);
        fields.add(age);

        // 自我介绍文本域
        FormField intro = new FormField();
        intro.setName("introduction");
        intro.setType("textarea");
        intro.setLabel("自我介绍");
        intro.setPlaceholder("请简单介绍一下自己");
        intro.setMaxLength(500);
        fields.add(intro);

        userForm.setFields(fields);

        // 渲染表单
        renderForm(userForm);

        // 模拟表单数据验证
        log.info("========== 表单数据验证 ==========");
        Map<String, Object> formData = new HashMap<>();
        formData.put("username", "zhangsan");
        formData.put("email", "zhangsan@example.com");
        formData.put("gender", "male");
        formData.put("education", "bachelor");
        formData.put("age", 25);

        ValidationResult validationResult = validate(userForm, formData);
        log.info("验证结果：{}", validationResult.isValid() ? "通过" : "失败");
        if (!validationResult.isValid()) {
            log.info("错误信息：{}", validationResult.getErrors());
        }
        log.info("==============================\n");
    }
}

/**
 * 表单定义
 */
@Data
class FormDefinition {
    private String title;           // 表单标题
    private String description;     // 表单描述
    private List<FormField> fields; // 字段列表
}

/**
 * 表单字段
 */
@Data
class FormField {
    private String name;            // 字段名
    private String type;            // 字段类型：input/textarea/select/radio/checkbox/date/number
    private String label;           // 字段标签
    private String placeholder;     // 占位符
    private boolean required;       // 是否必填
    private Integer maxLength;      // 最大长度
    private Map<String, String> options; // 选项（用于 select/radio/checkbox）
    private Double min;             // 最小值（用于 number）
    private Double max;             // 最大值（用于 number）
    private String step;            // 步长（用于 number）
}

/**
 * 验证结果
 */
@Data
class ValidationResult {
    private boolean valid;                  // 是否通过验证
    private Map<String, String> errors;     // 错误信息
}
