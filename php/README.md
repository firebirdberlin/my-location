my-location
===========

This php project is derived from the project C't longitude
by Oliver Lau.

https://code.google.com/p/ct-longitude/

Dependencies to Google services are stripped off the projects.
An encryption module MCrypt.php derived from

https://github.com/serpro/Android-PHP-Encrypt-Decrypt

is added to ensure AES encrypted communication to the android app.

Installation
============

 * Edit config.php
 * Set a password in MCrypt.php
 * copy the project folder to your webserver

 * Set the permissions, e.g. chown www-data:www-data *.php

 * Setup the database:
   http://your-webserver.com/path/to/my-location/install.php






