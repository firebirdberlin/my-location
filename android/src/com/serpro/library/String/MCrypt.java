/*
 *
 */
package com.serpro.library.String;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.firebirdberlin.longitude.SettingsActivity;

public class MCrypt {

		static char[] HEX_CHARS = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

		private IvParameterSpec ivspec;
		private SecretKeySpec keyspec;
		private Cipher cipher;
		private Context mContext = null;


		public MCrypt(Context context, String iv) {
			mContext = context;
			byte[] SecretKey = get_secret_key();

			ivspec = new IvParameterSpec(MCrypt.hexToBytes(iv));
			keyspec = new SecretKeySpec(SecretKey, "AES");

			try {
				cipher = Cipher.getInstance("AES/CBC/NoPadding");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			}
		}


		public MCrypt(Context context) {
			mContext = context;
			byte[] SecretKey = get_secret_key();
			byte[] iv = generate_iv();

			ivspec = new IvParameterSpec(iv);
			keyspec = new SecretKeySpec(SecretKey, "AES");

			try {
				cipher = Cipher.getInstance("AES/CBC/NoPadding");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			}
		}


		public byte[] encrypt(String text) throws Exception{
			if(text == null || text.length() == 0)
					throw new Exception("Empty string");

			byte[] encrypted = null;

			try {
				cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
				encrypted = cipher.doFinal(padString(text).getBytes());
			} catch (Exception e){
				throw new Exception("[encrypt] " + e.getMessage());
			}

			return encrypted;
		}


		public byte[] decrypt(String code) throws Exception{
			if(code == null || code.length() == 0)
					throw new Exception("Empty string");

			byte[] decrypted = null;

			try {
				cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);

				decrypted = cipher.doFinal(hexToBytes(code));
				//Remove trailing zeroes
				if( decrypted.length > 0)
				{
					int trim = 0;
					for( int i = decrypted.length - 1; i >= 0; i-- ) if( decrypted[i] == 0 ) trim++;

					if( trim > 0 )
					{
						byte[] newArray = new byte[decrypted.length - trim];
						System.arraycopy(decrypted, 0, newArray, 0, decrypted.length - trim);
						decrypted = newArray;
					}
				}
			} catch (Exception e){
				throw new Exception("[decrypt] " + e.getMessage());
			}
			return decrypted;
		}


		public static String bytesToHex(byte[] buf){
			char[] chars = new char[2 * buf.length];
			for (int i = 0; i < buf.length; ++i){
				chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
				chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
			}
			return new String(chars);
		}


		public static byte[] hexToBytes(String str) {
			if (str==null) {
					return null;
			} else if (str.length() < 2) {
					return null;
			} else {
					int len = str.length() / 2;
					byte[] buffer = new byte[len];
					for (int i=0; i<len; i++) {
							buffer[i] = (byte) Integer.parseInt(str.substring(i*2,i*2+2),16);
					}
					return buffer;
			}
		}


		private static String padString(String source){
			char paddingChar = 0;
			int size = 16;
			int x = source.length() % size;
			int padLength = size - x;

			for (int i = 0; i < padLength; i++){
				  source += paddingChar;
			}

			return source;
		}

		public static String encrypt_text(Context context, String plaintext){
			if (MCrypt.secret_key_is_valid(context)){
				MCrypt mcrypt = new MCrypt(context, "fedcba9876543210");

				try {
					MCrypt mcrypte = new MCrypt(context);
					String encrypted = MCrypt.bytesToHex( mcrypte.encrypt(plaintext));
					Log.d("LongitudeUpdater.Test", encrypted);
					MCrypt mcrypt2 = new MCrypt(context, mcrypte.get_iv());
					String decrypted = new String(mcrypt2.decrypt(encrypted));
					Log.d("LongitudeUpdater.Test", decrypted);


					return MCrypt.bytesToHex( mcrypt.encrypt(plaintext) );
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			return "";
		}


		public static String decrypt_text(Context context, String encrypted_text){
			if (MCrypt.secret_key_is_valid(context)){
				MCrypt mcrypt = new MCrypt(context, "fedcba9876543210");
				try{
					return new String(mcrypt.decrypt(encrypted_text));
				} catch(Exception e){
					e.printStackTrace();
				}
			}
			return "";
		}


		private byte[] get_secret_key(){
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
			return settings.getString(SettingsActivity.PREF_KEY_SERVER_PASSWORD, "").getBytes();
		}


		private byte[] generate_iv(){
			try{
				SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
				byte[] iv = new byte[16];
				sr.nextBytes(iv);
				return iv;
			} catch(NoSuchAlgorithmException e){
				e.printStackTrace();
			}
			return new String("fedcba9876543210").getBytes();
		}


		public String get_iv(){
			return MCrypt.bytesToHex(ivspec.getIV());
		}


		public static boolean secret_key_is_valid(Context context){
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
			String SecretKey = settings.getString(SettingsActivity.PREF_KEY_SERVER_PASSWORD, "");
			return (SecretKey.length() > 0);
		}
}
