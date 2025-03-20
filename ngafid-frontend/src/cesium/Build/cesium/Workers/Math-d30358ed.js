define(["exports","./when-c2e8ef35","./Check-c4f3a3fc"],function(t, r, n){"use strict";function i(t){null==t&&(t=(new Date).getTime()),this.N=624,this.M=397,this.MATRIX_A=2567483615,this.UPPER_MASK=2147483648,this.LOWER_MASK=2147483647,this.mt=new Array(this.N),this.mti=this.N+1,this.init_genrand(t)}i.prototype.init_genrand=function(t){for(this.mt[0]=t>>>0,this.mti=1; this.mti<this.N; this.mti++){t=this.mt[this.mti-1]^this.mt[this.mti-1]>>>30;this.mt[this.mti]=(1812433253*((4294901760&t)>>>16)<<16)+1812433253*(65535&t)+this.mti,this.mt[this.mti]>>>=0}},i.prototype.genrand_int32=function(){var t,n=new Array(0,this.MATRIX_A);if(this.mti>=this.N){var i;for(this.mti==this.N+1&&this.init_genrand(5489),i=0; i<this.N-this.M; i++)t=this.mt[i]&this.UPPER_MASK|this.mt[i+1]&this.LOWER_MASK,this.mt[i]=this.mt[i+this.M]^t>>>1^n[1&t];for(; i<this.N-1; i++)t=this.mt[i]&this.UPPER_MASK|this.mt[i+1]&this.LOWER_MASK,this.mt[i]=this.mt[i+(this.M-this.N)]^t>>>1^n[1&t];t=this.mt[this.N-1]&this.UPPER_MASK|this.mt[0]&this.LOWER_MASK,this.mt[this.N-1]=this.mt[this.M-1]^t>>>1^n[1&t],this.mti=0}return t=this.mt[this.mti++],t^=t>>>11,t^=t<<7&2636928640,t^=t<<15&4022730752,(t^=t>>>18)>>>0},i.prototype.random=function(){return this.genrand_int32()*(1/4294967296)};var h={EPSILON1:.1,EPSILON2:.01,EPSILON3:.001,EPSILON4:1e-4,EPSILON5:1e-5,EPSILON6:1e-6,EPSILON7:1e-7,EPSILON8:1e-8,EPSILON9:1e-9,EPSILON10:1e-10,EPSILON11:1e-11,EPSILON12:1e-12,EPSILON13:1e-13,EPSILON14:1e-14,EPSILON15:1e-15,EPSILON16:1e-16,EPSILON17:1e-17,EPSILON18:1e-18,EPSILON19:1e-19,EPSILON20:1e-20,EPSILON21:1e-21,GRAVITATIONALPARAMETER:3986004418e5,SOLAR_RADIUS:6955e5,LUNAR_RADIUS:1737400,SIXTY_FOUR_KILOBYTES:65536,FOUR_GIGABYTES:4294967296};h.sign=r.defaultValue(Math.sign,function(t){return 0===(t=+t)||t!=t?t:0<t?1:-1}),h.signNotZero=function(t){return t<0?-1:1},h.toSNorm=function(t, n){return n=r.defaultValue(n,255),Math.round((.5*h.clamp(t,-1,1)+.5)*n)},h.fromSNorm=function(t, n){return n=r.defaultValue(n,255),h.clamp(t,0,n)/n*2-1},h.normalize=function(t, n, i){return 0===(i=Math.max(i-n,0))?0:h.clamp((t-n)/i,0,1)},h.sinh=r.defaultValue(Math.sinh,function(t){return(Math.exp(t)-Math.exp(-t))/2}),h.cosh=r.defaultValue(Math.cosh,function(t){return(Math.exp(t)+Math.exp(-t))/2}),h.lerp=function(t, n, i){return(1-i)*t+i*n},h.PI=Math.PI,h.ONE_OVER_PI=1/Math.PI,h.PI_OVER_TWO=Math.PI/2,h.PI_OVER_THREE=Math.PI/3,h.PI_OVER_FOUR=Math.PI/4,h.PI_OVER_SIX=Math.PI/6,h.THREE_PI_OVER_TWO=3*Math.PI/2,h.TWO_PI=2*Math.PI,h.ONE_OVER_TWO_PI=1/(2*Math.PI),h.RADIANS_PER_DEGREE=Math.PI/180,h.DEGREES_PER_RADIAN=180/Math.PI,h.RADIANS_PER_ARCSECOND=h.RADIANS_PER_DEGREE/3600,h.toRadians=function(t){return t*h.RADIANS_PER_DEGREE},h.toDegrees=function(t){return t*h.DEGREES_PER_RADIAN},h.convertLongitudeRange=function(t){var n=h.TWO_PI,i=t-Math.floor(t/n)*n;return i<-Math.PI?i+n:i>=Math.PI?i-n:i},h.clampToLatitudeRange=function(t){return h.clamp(t,-1*h.PI_OVER_TWO,h.PI_OVER_TWO)},h.negativePiToPi=function(t){return h.zeroToTwoPi(t+h.PI)-h.PI},h.zeroToTwoPi=function(t){var n=h.mod(t,h.TWO_PI);return Math.abs(n)<h.EPSILON14&&Math.abs(t)>h.EPSILON14?h.TWO_PI:n},h.mod=function(t, n){return(t%n+n)%n},h.equalsEpsilon=function(t, n, i, a){a=r.defaultValue(a,i);var e=Math.abs(t-n);return e<=a||e<=i*Math.max(Math.abs(t),Math.abs(n))},h.lessThan=function(t, n, i){return t-n<-i},h.lessThanOrEquals=function(t, n, i){return t-n<i},h.greaterThan=function(t, n, i){return i<t-n},h.greaterThanOrEquals=function(t, n, i){return-i<t-n};var u=[1];h.factorial=function(t){var n=u.length;if(n<=t)for(var i=u[n-1],a=n; a<=t; a++){var e=i*a;u.push(e),i=e}return u[t]},h.incrementWrap=function(t, n, i){return i=r.defaultValue(i,0),n<++t&&(t=i),t},h.isPowerOfTwo=function(t){return 0!==t&&0==(t&t-1)},h.nextPowerOfTwo=function(t){return--t,t|=t>>1,t|=t>>2,t|=t>>4,t|=t>>8,t|=t>>16,++t},h.clamp=function(t, n, i){return t<n?n:i<t?i:t};var a=new i;h.setRandomNumberSeed=function(t){a=new i(t)},h.nextRandomNumber=function(){return a.random()},h.randomBetween=function(t, n){return h.nextRandomNumber()*(n-t)+t},h.acosClamped=function(t){return Math.acos(h.clamp(t,-1,1))},h.asinClamped=function(t){return Math.asin(h.clamp(t,-1,1))},h.chordLength=function(t, n){return 2*n*Math.sin(.5*t)},h.logBase=function(t, n){return Math.log(t)/Math.log(n)},h.cbrt=r.defaultValue(Math.cbrt,function(t){var n=Math.pow(Math.abs(t),1/3);return t<0?-n:n}),h.log2=r.defaultValue(Math.log2,function(t){return Math.log(t)*Math.LOG2E}),h.fog=function(t, n){var i=t*n;return 1-Math.exp(-i*i)},h.fastApproximateAtan=function(t){return t*(-.1784*Math.abs(t)-.0663*t*t+1.0301)},h.fastApproximateAtan2=function(t, n){var i,a,e=Math.abs(t);i=Math.abs(n),a=Math.max(e,i);var r=(i=Math.min(e,i))/a;return e=h.fastApproximateAtan(r),e=Math.abs(n)>Math.abs(t)?h.PI_OVER_TWO-e:e,e=t<0?h.PI-e:e,e=n<0?-e:e},t.CesiumMath=h});
