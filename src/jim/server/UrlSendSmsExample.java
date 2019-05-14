package jim.server;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本範例提供了一個發送 [智邦一元 PP 簡訊] 的包裝類別，UrlSmsSender，以及它的使用範例
 * 
 * UrlSmsSender 有: 
 *   - 一個建構元: UrlSmsSender(apiKey, username, password)
 *   - 設定 API 金鑰、帳號、密碼的方法: setApiKey(apiKey)、setUsername(username)、setPassword(password)
 *   - 三種發送簡訊的方法：
 *       send(smsList, smsBody): 立即傳送一則簡訊
 *       send(smsList, smsBody, smsTime): 在預定的 smsTime 傳送一則簡訊
 *
 * 詳細資訊請參考各方法上方的說明
 */
public class UrlSendSmsExample {
	
	static final String MSG = "Test From Java"; 
    /**
     * UrlSmsSender 的使用範例
     * 使用 UrlSmsSender 立即傳送一則簡訊，預約 15 分鐘後傳送一則簡訊，
     * 以及如何讀取主機回傳訊息的範例。
     */
    public static void main(String[] args) throws IOException {
        String apiKey = "92980b74d20dd37f91a32ac37dd1f6c8";
        String username = "LABUSE";
        String password = "LABUSE20162017";
        
        UrlSmsSender smsSender = new UrlSmsSender(apiKey, username, password);
        
        // 建立一個陣列放置簡訊的接收門號，一次最多可傳送給 250 個號碼
        String[] smsList = new String[] {"0921780245"};
        
        
        // 立即傳送一則簡訊，並取得主機回傳的訊息。有哪些資訊可取得請參考:
        // http://pp.url.com.tw/option/api [回傳參數] 的說明
        HashMap<String, String> response1 = smsSender.sendNow(smsList, MSG);
        System.out.println("立即傳送 - 狀態: " + response1.get("status"));
        System.out.println("立即傳送 - 訊息: " + response1.get("message"));
        
        
//        // 預約 15 分鐘後傳送一則簡訊 - 使用 Calender 類別
//        Calendar future = Calendar.getInstance();
//        future.add(Calendar.MINUTE, 15);
//        
//        HashMap<String, String> response2 = smsSender.sendScheduled(smsList, 
//                "智邦 PP 簡訊 API - Java 範例\n預約傳送的訊息\n這封簡訊是在 15 分鐘前預約發送", future);
//        System.out.println("預約傳送 - 狀態: " + response2.get("status"));
//        System.out.println("預約傳送 - 訊息: " + response2.get("message"));
    }

    
    
    /**
     * 負責發送智邦 PP 一元簡訊的類別。
     */
    public static class UrlSmsSender {
        private static final String API_URL ="http://pp.url.com.tw/api/msg";

        private String apiKey;
        private String username;
        private String password;
        
        private String parameterFirstHalf;

        /**
         * 建立一個發送智邦 PP 一元簡訊的物件。
         * @param apiKey 簡訊發送 API 金鑰。
         * @param username 智邦會員中心帳號。
         * @param password 智邦會員中心密碼。
         */
        public UrlSmsSender(String apiKey, String username, String password) {
            if(apiKey == null || apiKey.length() < 1)
                throw new IllegalArgumentException("apiKey is empty");
            if(username == null || username.length() < 1)
                throw new IllegalArgumentException("username is empty");
            if(password == null || password.length() < 1)
                throw new IllegalArgumentException("password is empty");
            
            this.apiKey = apiKey;
            this.username = username;
            this.password = password;
            
            generateParameterFirstHalf();
        }
        
        /**
         * 設定簡訊發送 API 金鑰。
         */
        public void setApiKey(String apiKey) {
            if(apiKey == null || apiKey.length() < 1)
                throw new IllegalArgumentException("apiKey is empty");
            
            this.apiKey = apiKey;
            generateParameterFirstHalf();
        }

        /**
         * 設定智邦會員中心帳號。
         */
        public void setUsername(String username) {
            if(username == null || username.length() < 1)
                throw new IllegalArgumentException("username is empty");
            
            this.username = username;
            generateParameterFirstHalf();
        }
        
        /**
         * 設定智邦會員中心密碼。
         */
        public void setPassword(String password) {
            if(password == null || password.length() < 1)
                throw new IllegalArgumentException("password is empty");
            
            this.password = password;
            generateParameterFirstHalf();
        }
        
        /**
         * 立即傳送一則簡訊。
         * @param smsList 簡訊的接收門號。最多可達 250 個。
         * @param smsBody 簡訊的內容，一則簡訊最多可傳送 140 個字元。
         *                其中一個中文字佔 2 個字元，一個英文字佔 1 個字元，一個換行符號佔 1 個字元。
         * @return 主機的回傳訊息。
         * @throws IOException
         */
        public HashMap<String, String> sendNow(String[] smsList, String smsBody) throws IOException {
            String s = null;
            return sendScheduled(smsList, smsBody, s);
        }
        
        /**
         * 在特定的時間傳送一則簡訊。
         * @param smsList 簡訊的接收門號。最多可達 250 個。
         * @param smsBody 簡訊的內容，一則簡訊最多可傳送 140 個字元。
         *                其中一個中文字佔 2 個字元，一個英文字佔 1 個字元，一個換行符號佔 1 個字元。
         * @param smsTime 預約傳送的時間；若為 null 則表示立即發送。
         * @return 主機的回傳訊息。
         * @throws IOException
         */
        public HashMap<String, String> sendScheduled(String[] smsList, String smsBody, Calendar smsTime) throws IOException {
            String translatedTime = null;
            
            if(smsTime != null) {
                SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                translatedTime = dateformat.format(smsTime.getTime());
            }
            
            return sendScheduled(smsList, smsBody, translatedTime);
        }

        /**
         * 在特定的時間傳送一則簡訊。
         * @param smsList 簡訊的接收門號。最多可達 250 個。
         * @param smsBody 簡訊的內容，一則簡訊最多可傳送 140 個字元。
         *                其中一個中文字佔 2 個字元，一個英文字佔 1 個字元，一個換行符號佔 1 個字元。
         * @param smsTime 預約傳送的時間，格式為 yyyy-MM-dd HH:mm:ss；若為 null 或空字串則表示立即發送。
         * @return 主機的回傳訊息。
         * @throws IOException
         */
        public HashMap<String, String> sendScheduled(String[] smsList, String smsBody, String smsTime) throws IOException {
            String parameters = createParametersString(smsList, smsBody, smsTime);
            
            URL url = null;
            try {
                url = new URL(API_URL);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            
            sendParameters(connection, parameters);
            String response = getResponse(connection);
            
            connection.disconnect();

            return parseResponse(response);
        }
        
        /**
         * 產生前半段的 POST 參數
         */
        private void generateParameterFirstHalf() {
            parameterFirstHalf = "api_key=" + apiKey + "&user_name=" + username 
                    + "&password=" + password;
        }
        
        /**
         * 將參數串成一個字串。
         */
        private String createParametersString(String[] addresses, String message, String time) {
            if(addresses == null || addresses.length < 1)
                throw new IllegalArgumentException("No addresses.");
            if(message == null || message.length() < 1)
                throw new IllegalArgumentException("No message content.");
            
            StringBuilder params = new StringBuilder(parameterFirstHalf);

            params.append("&sms_list=");
            for(String number : addresses) {
                if(number != null && number.length() > 0)
                    params.append(number).append("%2c");
            }

            try {
                params.append("&sms_body=").append(URLEncoder.encode(message, "UTF-8"));
                
                if(time != null && time.length() > 0) {
                    params.append("&sms_time=").append(URLEncoder.encode(time, "UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            
            return params.toString();
        }

        /**
         * 傳送參數至伺服器。
         */
        private void sendParameters(HttpURLConnection connection, String parameters) throws IOException {
            try {
                connection.setRequestMethod("POST");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }

            byte[] parametersBytes = parameters.getBytes();
            
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", Integer.toString(parametersBytes.length));
            connection.setUseCaches(false);

            BufferedOutputStream bos = new BufferedOutputStream(connection.getOutputStream());
            bos.write(parametersBytes);
            bos.flush();
            bos.close();
        }

        /**
         * 從伺服器接收回傳訊息。
         */
        private String getResponse(HttpURLConnection connection) throws IOException {
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));

            String inputLine;
            while((inputLine = br.readLine()) != null) {
                stringBuilder.append(inputLine);
            }

            br.close();
            return stringBuilder.toString();
        }

        /**
         * 解析回應訊息。
         */
        private static HashMap<String, String> parseResponse(String response) {
            HashMap<String, String> result = new HashMap<String, String>();
            
            String pattern = "('[^']*')";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(response);

            String key = null;
            String value = null;

            while(m.find()) {
                key = m.group();
                key = key.substring(1, key.length() - 1);
                m.find();
                
                value = m.group();
                value = value.substring(1, value.length() - 1);
                result.put(key, value);
            }

            return result;
        }
    }

}
