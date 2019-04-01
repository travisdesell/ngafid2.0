# ngafid2.0


airport database:
http://osav-usdot.opendata.arcgis.com/datasets/a36a509eab4e43b4864fb594a35b90d6_0?filterByExtent=false&geometry=-97.201%2C47.944%2C-97.147%2C47.952

runway database:
http://osav-usdot.opendata.arcgis.com/datasets/d1b43f8a1d474b8c9c24cad4b942b74a_0?uiTab=table&geometry=-97.2%2C47.944%2C-97.146%2C47.953&filterByExtent=false


required for jQuery query-builder:

https://github.com/mistic100/jQuery.extendext.git
https://github.com/olado/doT.git


setting up javascript with react/webpack/babel:
https://www.valentinog.com/blog/react-webpack-babel/



#not used anymore
information on using PM2 to start/restart node servers:

https://www.digitalocean.com/community/tutorials/how-to-set-up-a-node-js-application-for-production-on-centos-7

information on setting up apache to use PM2:

https://vedmant.com/setup-node-js-production-application-apache-multiple-virtual-host-server/

if error "service unavailabile":
http://sysadminsjourney.com/content/2010/02/01/apache-modproxy-error-13permission-denied-error-rhel/

to fix:

sudo /usr/sbin/setsebool -P httpd_can_network_connect 1
