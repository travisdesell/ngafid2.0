define(["exports","./when-c2e8ef35","./Check-c4f3a3fc","./Math-d30358ed","./Cartesian2-e875d9d2"],function(t, w, a, T, b){"use strict";function z(t, a, i, n, e, s, r){var h,o,d=(h=t)*(o=i)*(4+h*(4-3*o))/16;return(1-d)*t*a*(n+d*e*(r+d*s*(2*r*r-1)))}var y=new b.Cartesian3,E=new b.Cartesian3;function e(t, a, i, n){var e,s,r,h,o,d,c,u,M,l,g,_,p,f,v,m,C,H,O,S,q,U,w,A,R;b.Cartesian3.normalize(n.cartographicToCartesian(a,E),y),b.Cartesian3.normalize(n.cartographicToCartesian(i,E),E);!function(t, a, i, n, e, s, r){var h,o,d,c,u,M=(a-i)/a,l=s-n,g=Math.atan((1-M)*Math.tan(e)),_=Math.atan((1-M)*Math.tan(r)),p=Math.cos(g),f=Math.sin(g),v=Math.cos(_),m=Math.sin(_),C=p*v,H=p*m,O=f*m,S=f*v,q=l,U=T.CesiumMath.TWO_PI,w=Math.cos(q),A=Math.sin(q);do{w=Math.cos(q),A=Math.sin(q);var R,b=H-S*w;d=Math.sqrt(v*v*A*A+b*b),o=O+C*w,h=Math.atan2(d,o),U=q,u=o-2*O/(c=0===d?(R=0,1):1-(R=C*A/d)*R),isNaN(u)&&(u=0),q=l+z(M,R,c,h,d,o,u)}while(Math.abs(q-U)>T.CesiumMath.EPSILON12);var y=c*(a*a-i*i)/(i*i),E=y*(256+y*(y*(74-47*y)-128))/1024,P=u*u,x=i*(1+y*(4096+y*(y*(320-175*y)-768))/16384)*(h-E*d*(u+E*(o*(2*P-1)-E*u*(4*d*d-3)*(4*P-3)/6)/4)),D=Math.atan2(v*A,H-S*w),N=Math.atan2(p*A,H*w-S);t._distance=x,t._startHeading=D,t._endHeading=N,t._uSquared=y}(t,n.maximumRadius,n.minimumRadius,a.longitude,a.latitude,i.longitude,i.latitude),t._start=b.Cartographic.clone(a,t._start),t._end=b.Cartographic.clone(i,t._end),t._start.height=0,t._end.height=0,s=(e=t)._uSquared,r=e._ellipsoid.maximumRadius,h=e._ellipsoid.minimumRadius,o=(r-h)/r,d=Math.cos(e._startHeading),c=Math.sin(e._startHeading),u=(1-o)*Math.tan(e._start.latitude),M=1/Math.sqrt(1+u*u),l=M*u,g=Math.atan2(u,d),f=1-(p=(_=M*c)*_),v=Math.sqrt(f),U=1-3*(m=s/4)+35*(C=m*m)/4,w=1-5*m,A=(S=1+m-3*C/4+5*(H=C*m)/4-175*(O=C*C)/64)*g-(q=1-m+15*C/8-35*H/8)*Math.sin(2*g)*m/2-U*Math.sin(4*g)*C/16-w*Math.sin(6*g)*H/48-5*Math.sin(8*g)*O/512,(R=e._constants).a=r,R.b=h,R.f=o,R.cosineHeading=d,R.sineHeading=c,R.tanU=u,R.cosineU=M,R.sineU=l,R.sigma=g,R.sineAlpha=_,R.sineSquaredAlpha=p,R.cosineSquaredAlpha=f,R.cosineAlpha=v,R.u2Over4=m,R.u4Over16=C,R.u6Over64=H,R.u8Over256=O,R.a0=S,R.a1=q,R.a2=U,R.a3=w,R.distanceRatio=A}function i(t, a, i){var n=w.defaultValue(i,b.Ellipsoid.WGS84);this._ellipsoid=n,this._start=new b.Cartographic,this._end=new b.Cartographic,this._constants={},this._startHeading=void 0,this._endHeading=void 0,this._distance=void 0,this._uSquared=void 0,w.defined(t)&&w.defined(a)&&e(this,t,a,n)}Object.defineProperties(i.prototype,{ellipsoid:{get:function(){return this._ellipsoid}},surfaceDistance:{get:function(){return this._distance}},start:{get:function(){return this._start}},end:{get:function(){return this._end}},startHeading:{get:function(){return this._startHeading}},endHeading:{get:function(){return this._endHeading}}}),i.prototype.setEndPoints=function(t, a){e(this,t,a,this._ellipsoid)},i.prototype.interpolateUsingFraction=function(t, a){return this.interpolateUsingSurfaceDistance(this._distance*t,a)},i.prototype.interpolateUsingSurfaceDistance=function(t, a){var i=this._constants,n=i.distanceRatio+t/i.b,e=Math.cos(2*n),s=Math.cos(4*n),r=Math.cos(6*n),h=Math.sin(2*n),o=Math.sin(4*n),d=Math.sin(6*n),c=Math.sin(8*n),u=n*n,M=n*u,l=i.u8Over256,g=i.u2Over4,_=i.u6Over64,p=i.u4Over16,f=2*M*l*e/3+n*(1-g+7*p/4-15*_/4+579*l/64-(p-15*_/4+187*l/16)*e-(5*_/4-115*l/16)*s-29*l*r/16)+(g/2-p+71*_/32-85*l/16)*h+(5*p/16-5*_/4+383*l/96)*o-u*((_-11*l/2)*h+5*l*o/2)+(29*_/96-29*l/16)*d+539*l*c/1536,v=Math.asin(Math.sin(f)*i.cosineAlpha),m=Math.atan(i.a/i.b*Math.tan(v));f-=i.sigma;var C=Math.cos(2*i.sigma+f),H=Math.sin(f),O=Math.cos(f),S=i.cosineU*O,q=i.sineU*H,U=Math.atan2(H*i.sineHeading,S-q*i.cosineHeading)-z(i.f,i.sineAlpha,i.cosineSquaredAlpha,f,H,O,C);return w.defined(a)?(a.longitude=this._start.longitude+U,a.latitude=m,a.height=0,a):new b.Cartographic(this._start.longitude+U,m,0)},t.EllipsoidGeodesic=i});
