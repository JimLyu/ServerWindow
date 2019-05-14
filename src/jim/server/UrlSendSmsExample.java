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
 * ���d�Ҵ��ѤF�@�ӵo�e [�����@�� PP ²�T] ���]�����O�AUrlSmsSender�A�H�Υ����ϥνd��
 * 
 * UrlSmsSender ��: 
 *   - �@�ӫغc��: UrlSmsSender(apiKey, username, password)
 *   - �]�w API ���_�B�b���B�K�X����k: setApiKey(apiKey)�BsetUsername(username)�BsetPassword(password)
 *   - �T�صo�e²�T����k�G
 *       send(smsList, smsBody): �ߧY�ǰe�@�h²�T
 *       send(smsList, smsBody, smsTime): �b�w�w�� smsTime �ǰe�@�h²�T
 *
 * �ԲӸ�T�аѦҦU��k�W�誺����
 */
public class UrlSendSmsExample {
	
	static final String MSG = "Test From Java"; 
    /**
     * UrlSmsSender ���ϥνd��
     * �ϥ� UrlSmsSender �ߧY�ǰe�@�h²�T�A�w�� 15 ������ǰe�@�h²�T�A
     * �H�Φp��Ū���D���^�ǰT�����d�ҡC
     */
    public static void main(String[] args) throws IOException {
        String apiKey = "92980b74d20dd37f91a32ac37dd1f6c8";
        String username = "LABUSE";
        String password = "LABUSE20162017";
        
        UrlSmsSender smsSender = new UrlSmsSender(apiKey, username, password);
        
        // �إߤ@�Ӱ}�C��m²�T�����������A�@���̦h�i�ǰe�� 250 �Ӹ��X
        String[] smsList = new String[] {"0921780245"};
        
        
        // �ߧY�ǰe�@�h²�T�A�è��o�D���^�Ǫ��T���C�����Ǹ�T�i���o�аѦ�:
        // http://pp.url.com.tw/option/api [�^�ǰѼ�] ������
        HashMap<String, String> response1 = smsSender.sendNow(smsList, MSG);
        System.out.println("�ߧY�ǰe - ���A: " + response1.get("status"));
        System.out.println("�ߧY�ǰe - �T��: " + response1.get("message"));
        
        
//        // �w�� 15 ������ǰe�@�h²�T - �ϥ� Calender ���O
//        Calendar future = Calendar.getInstance();
//        future.add(Calendar.MINUTE, 15);
//        
//        HashMap<String, String> response2 = smsSender.sendScheduled(smsList, 
//                "���� PP ²�T API - Java �d��\n�w���ǰe���T��\n�o��²�T�O�b 15 �����e�w���o�e", future);
//        System.out.println("�w���ǰe - ���A: " + response2.get("status"));
//        System.out.println("�w���ǰe - �T��: " + response2.get("message"));
    }

    
    
    /**
     * �t�d�o�e���� PP �@��²�T�����O�C
     */
    public static class UrlSmsSender {
        private static final String API_URL ="http://pp.url.com.tw/api/msg";

        private String apiKey;
        private String username;
        private String password;
        
        private String parameterFirstHalf;

        /**
         * �إߤ@�ӵo�e���� PP �@��²�T������C
         * @param apiKey ²�T�o�e API ���_�C
         * @param username �����|�����߱b���C
         * @param password �����|�����߱K�X�C
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
         * �]�w²�T�o�e API ���_�C
         */
        public void setApiKey(String apiKey) {
            if(apiKey == null || apiKey.length() < 1)
                throw new IllegalArgumentException("apiKey is empty");
            
            this.apiKey = apiKey;
            generateParameterFirstHalf();
        }

        /**
         * �]�w�����|�����߱b���C
         */
        public void setUsername(String username) {
            if(username == null || username.length() < 1)
                throw new IllegalArgumentException("username is empty");
            
            this.username = username;
            generateParameterFirstHalf();
        }
        
        /**
         * �]�w�����|�����߱K�X�C
         */
        public void setPassword(String password) {
            if(password == null || password.length() < 1)
                throw new IllegalArgumentException("password is empty");
            
            this.password = password;
            generateParameterFirstHalf();
        }
        
        /**
         * �ߧY�ǰe�@�h²�T�C
         * @param smsList ²�T�����������C�̦h�i�F 250 �ӡC
         * @param smsBody ²�T�����e�A�@�h²�T�̦h�i�ǰe 140 �Ӧr���C
         *                �䤤�@�Ӥ���r�� 2 �Ӧr���A�@�ӭ^��r�� 1 �Ӧr���A�@�Ӵ���Ÿ��� 1 �Ӧr���C
         * @return �D�����^�ǰT���C
         * @throws IOException
         */
        public HashMap<String, String> sendNow(String[] smsList, String smsBody) throws IOException {
            String s = null;
            return sendScheduled(smsList, smsBody, s);
        }
        
        /**
         * �b�S�w���ɶ��ǰe�@�h²�T�C
         * @param smsList ²�T�����������C�̦h�i�F 250 �ӡC
         * @param smsBody ²�T�����e�A�@�h²�T�̦h�i�ǰe 140 �Ӧr���C
         *                �䤤�@�Ӥ���r�� 2 �Ӧr���A�@�ӭ^��r�� 1 �Ӧr���A�@�Ӵ���Ÿ��� 1 �Ӧr���C
         * @param smsTime �w���ǰe���ɶ��F�Y�� null �h��ܥߧY�o�e�C
         * @return �D�����^�ǰT���C
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
         * �b�S�w���ɶ��ǰe�@�h²�T�C
         * @param smsList ²�T�����������C�̦h�i�F 250 �ӡC
         * @param smsBody ²�T�����e�A�@�h²�T�̦h�i�ǰe 140 �Ӧr���C
         *                �䤤�@�Ӥ���r�� 2 �Ӧr���A�@�ӭ^��r�� 1 �Ӧr���A�@�Ӵ���Ÿ��� 1 �Ӧr���C
         * @param smsTime �w���ǰe���ɶ��A�榡�� yyyy-MM-dd HH:mm:ss�F�Y�� null �ΪŦr��h��ܥߧY�o�e�C
         * @return �D�����^�ǰT���C
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
         * ���ͫe�b�q�� POST �Ѽ�
         */
        private void generateParameterFirstHalf() {
            parameterFirstHalf = "api_key=" + apiKey + "&user_name=" + username 
                    + "&password=" + password;
        }
        
        /**
         * �N�ѼƦꦨ�@�Ӧr��C
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
         * �ǰe�ѼƦܦ��A���C
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
         * �q���A�������^�ǰT���C
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
         * �ѪR�^���T���C
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
