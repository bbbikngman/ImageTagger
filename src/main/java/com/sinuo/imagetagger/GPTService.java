package com.sinuo.imagetagger;

import static com.sinuo.imagetagger.utils.ContextUtils.getMyApplicationContext;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import android.util.Log;

import com.sinuo.imagetagger.utils.ImageUtils;
import com.sinuo.imagetagger.utils.SettingsManager;

public class GPTService {
    private static final String TAG = "GPTService"; // 添加TAG用于日志

    public static void sendImageToGPT(Bitmap image, Runnable showSettings, ResponseCallback callback) {
        // 获取设置
        Context context = getMyApplicationContext();
        String apiUrl = SettingsManager.getSetting(context, "gptApiUrl", "https://default.api.url");
        String apiKey = SettingsManager.getSetting(context, "gptApiKey", "");
        String languageA = SettingsManager.getSetting(context, "languageA", "English");
        String languageB = SettingsManager.getSetting(context, "languageB", "Japanese");
        String connectionType = SettingsManager.getSetting(context, "connectionType", "direct");

        // 打印设置信息
        Log.d(TAG, "Settings loaded:");
        Log.d(TAG, "API URL: " + apiUrl);
        Log.d(TAG, "Language A: " + languageA);
        Log.d(TAG, "Language B: " + languageB);
        Log.d(TAG, "Connection Type: " + connectionType);
        Log.d(TAG, "API Key length: " + (apiKey.isEmpty() ? "0" : apiKey.length())); // 安全起见只打印长度

        if (apiUrl.isEmpty()) {
            Log.e(TAG, "Invalid API URL");
            callback.onFailure("API URL is empty");
            if (showSettings != null) {
                showSettings.run();
            }
            return;
        }

        // 图像处理
        String base64Image = ImageUtils.bitmapToBase64(image, 40);
        int width = image.getWidth();
        int height = image.getHeight();
        Log.d(TAG, String.format("Image dimensions: %dx%d, Base64 length: %d", width, height, base64Image.length()));

        @SuppressLint("DefaultLocale") String promptText = String.format(
                "Please name the objects in this picture in both %s and %s, and give the basic xy coordinates range of each object (%d*%d), describe the total atmosphere.\n" +
                        "The format should be like this, xy coordinates are necessary(if you don't know what they really are give me random one ):\n" +
                        "In this picture with height: h and width: w, there are:\n" +
                        "1. Object1InLanguageA ObjectInLanguageB: from (x1, y1) to (x2, y2)\n" +
                        "...\n" +
                        "atmosphere:\n" +
                        "A: description words in language A.\n" +
                        "B: description words in language B.\n",
                languageA, languageB, width, height
        );

        try {
            // 构建请求JSON
            JSONArray messages = new JSONArray();
            JSONObject textMessage = new JSONObject();
            textMessage.put("type", "text");
            textMessage.put("text", promptText);

            JSONObject imageMessage = new JSONObject();
            imageMessage.put("type", "image_url");
            imageMessage.put("image_url", new JSONObject().put("url", "data:image/jpeg;base64," + base64Image));

            messages.put(textMessage);
            messages.put(imageMessage);

            JSONObject json = new JSONObject();
            json.put("model", "gpt-4-vision-preview");  // 修正模型名称
            json.put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", messages)));
            json.put("max_tokens", 500);  // 添加token限制

            // 打印请求信息（不包含图片数据）
            Log.d(TAG, "Request JSON structure (excluding image data):");
            Log.d(TAG, "Model: " + json.getString("model"));
            Log.d(TAG, "Prompt: " + promptText);

            // 设置连接
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(30000); // 30秒连接超时
            connection.setReadTimeout(30000);    // 30秒读取超时

            if (connectionType.equals("direct")) {
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                Log.d(TAG, "Using direct connection with Bearer token");
            } else {
                connection.setRequestProperty("Authorization", apiKey);
                Log.d(TAG, "Using proxy connection");
            }
            connection.setDoOutput(true);

            // 发送请求
            try (DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
                writer.write(json.toString().getBytes());
                writer.flush();
                Log.d(TAG, "Request sent successfully");
            }

            // 处理响应
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d(TAG, "Raw response: " + response);

                JSONObject responseJson = new JSONObject(response.toString());
                JSONArray choices = responseJson.optJSONArray("choices");
                if (choices != null && choices.length() > 0) {
                    JSONObject message = choices.getJSONObject(0).optJSONObject("message");
                    if (message != null) {
                        String content = message.optString("content", "No message content found");

                        List<TaggedObject> taggedObjects = parseGPTResponse(content, height, width);

                        // 打印解析结果
                        Log.d(TAG, "Parsed response:");
                        Log.d(TAG, "Content: " + content);
                        Log.d(TAG, "Tagged objects count: " + taggedObjects.size());

                        callback.onSuccess(content, taggedObjects);
                    } else {
                        String error = "No valid message found in response";
                        Log.e(TAG, error);
                        callback.onFailure(error);
                    }
                } else {
                    String error = "No valid choices found in response";
                    Log.e(TAG, error);
                    callback.onFailure(error);
                }
            } else {
                // 错误响应处理
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();

                String error = "Request failed with code: " + responseCode + ", Error: " + errorResponse;
                Log.e(TAG, error);
                callback.onFailure(error);
            }
        } catch (Exception e) {
            String error = "Error occurred: " + e.getMessage();
            Log.e(TAG, error, e);
            e.printStackTrace();
            callback.onFailure(error);
        }
    }

    private static List<TaggedObject> parseGPTResponse(String response, int height, int width) {
        List<TaggedObject> taggedObjects = new ArrayList<>();
        String[] lines = response.split("\n");
        Log.d(TAG, "Parsing response, total lines: " + lines.length);

        for (String line : lines) {

            line = line.trim();
            Log.d(TAG, "Processing line: " + line);

            // 跳过atmosphere部分
            if (line.toLowerCase().contains("atmosphere:")) {
                Log.d(TAG, "Reached atmosphere section, stopping parse");
                break;
            }

            // 移除数字编号
            line = line.replaceAll("^\\d+\\.", "");
            if (line.isEmpty()) continue;

            String[] parts = line.split(":");
            if (parts.length != 2) {
                Log.d(TAG, "Skipping invalid line format");
                continue;
            }

            String combinedName = parts[0];
            String coordinatesString = parts[1].trim();

            String keyword = "from";

            // 检查是否包含 "from"
            int index = coordinatesString.indexOf(keyword);

            if (index == -1) {
                // 如果没有 "from"，跳过当前处理
                continue;
            } else {
                // 如果有 "from"，保留从 "from" 开始的部分
                coordinatesString = coordinatesString.substring(index);
            }

            try {
                // 提取坐标
                String[] coordParts = coordinatesString
                        .replace("from", "")
                        .replace("(", "")
                        .replace(")", "")
                        .replace("to", ",")
                        .split(",");

                if (coordParts.length == 4) {
                    int[] coordinates = new int[4];
                    boolean haveDetailedCoordinate = true;
                    for (int i = 0; i < 4; i++) {
                        try {
                            coordinates[i] = Integer.parseInt(coordParts[i].trim());
                        } catch (Exception e) {
                            haveDetailedCoordinate = false;
                        }
                    }
                    if(!haveDetailedCoordinate) {
                        continue;
                    }
                    TaggedObject taggedObject = new TaggedObject(combinedName,
                            coordinates[0], coordinates[1], coordinates[2], coordinates[3], height, width);
                    taggedObjects.add(taggedObject);

                    Log.d(TAG, String.format("Successfully parsed object: %s at (%d,%d) to (%d,%d) in picture(%d,%d)",
                            combinedName, coordinates[0], coordinates[1], coordinates[2], coordinates[3], height, width));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing coordinates for line: " + line, e);
            }
        }

        Log.d(TAG, "Parsed " + taggedObjects.size() + " objects successfully");
        return taggedObjects;
    }

    public interface ResponseCallback {
        void onSuccess(String content, List<TaggedObject> taggedObjects);
        void onFailure(String error);
    }
}