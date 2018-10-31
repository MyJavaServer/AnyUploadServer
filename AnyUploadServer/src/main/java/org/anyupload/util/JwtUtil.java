package org.anyupload.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import net.sf.json.JSONObject;
import org.anyupload.entity.UserToken;

import javax.xml.bind.DatatypeConverter;

/**
 * JSON WEB TOKEN
 */
public class JwtUtil {

    public static final String KEY_TOKEN = "token";



    /**
     * 解密
     *
     * @param token
     * @param secrete
     * @return
     */
    public static UserToken parseJWT(String token, String secrete) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(DatatypeConverter.parseBase64Binary(secrete))
                    .parseClaimsJws(token).getBody();

            String tokenStr = claims.get(KEY_TOKEN).toString();

            return (UserToken) JSONObject.toBean(JSONObject.fromObject(tokenStr),UserToken.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
