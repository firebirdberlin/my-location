<?php
/**
 * Thankfully derived from
 *
 * https://github.com/serpro/Android-PHP-Encrypt-Decrypt
 *
 **/

class MCrypt
{
    // change it !!!
    private $key = '0123456789abcdef';

    function __construct()
    {
    }

    /**
     * @param string $str
     * @param bool $isBinary whether to encrypt as binary or not. Default is: false
     * @return string Encrypted data
     */
    function encrypt($str, $isBinary = false)
    {
        $iv_byte = openssl_random_pseudo_bytes(8);
        $iv = bin2hex($iv_byte);

        $str = $isBinary ? $str : utf8_decode($str);

        $td = mcrypt_module_open('rijndael-128', ' ', 'cbc', $iv);

        mcrypt_generic_init($td, $this->key, $iv);
        $encrypted = mcrypt_generic($td, $str);

        mcrypt_generic_deinit($td);
        mcrypt_module_close($td);
        $encrypted = $isBinary ? $encrypted : bin2hex($encrypted);
        return $iv + $encrypted;
    }

    /**
     * @param string $code
     * @param bool $isBinary whether to decrypt as binary or not. Default is: false
     * @return string Decrypted data
     */
    function decrypt($code, $isBinary = false)
    {
        # determine iv
        $iv = substr($code, 0, 32);
        $iv = $this->hex2bin($iv);

        $code = $isBinary ? $code : $this->hex2bin($code);
        $td = mcrypt_module_open('rijndael-128', ' ', 'cbc', $iv);

        mcrypt_generic_init($td, $this->key, $iv);
        $decrypted = mdecrypt_generic($td, $code);

        mcrypt_generic_deinit($td);
        mcrypt_module_close($td);

        return $isBinary ? trim($decrypted) : utf8_encode(trim($decrypted));
    }

    protected function hex2bin($hexdata)
    {
        $bindata = '';

        for ($i = 0; $i < strlen($hexdata); $i += 2) {
            $bindata .= chr(hexdec(substr($hexdata, $i, 2)));
        }

        return $bindata;
    }

}
