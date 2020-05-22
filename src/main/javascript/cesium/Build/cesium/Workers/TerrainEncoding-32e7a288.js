define(["exports","./when-c2e8ef35","./Check-c4f3a3fc","./Math-d30358ed","./Cartesian2-e875d9d2","./Transforms-44592b02","./ComponentDatatype-5d3f6452","./AttributeCompression-6cb5b251"],function(t,y,e,f,b,v,s,x){"use strict";function i(t,e){this._ellipsoid=t,this._cameraPosition=new b.Cartesian3,this._cameraPositionInScaledSpace=new b.Cartesian3,this._distanceToLimbInScaledSpaceSquared=0,y.defined(e)&&(this.cameraPosition=e)}Object.defineProperties(i.prototype,{ellipsoid:{get:function(){return this._ellipsoid}},cameraPosition:{get:function(){return this._cameraPosition},set:function(t){var e=this._ellipsoid.transformPositionToScaledSpace(t,this._cameraPositionInScaledSpace),i=b.Cartesian3.magnitudeSquared(e)-1;b.Cartesian3.clone(t,this._cameraPosition),this._cameraPositionInScaledSpace=e,this._distanceToLimbInScaledSpaceSquared=i}}});var m=new b.Cartesian3;i.prototype.isPointVisible=function(t){return h(this._ellipsoid.transformPositionToScaledSpace(t,m),this._cameraPositionInScaledSpace,this._distanceToLimbInScaledSpaceSquared)},i.prototype.isScaledSpacePointVisible=function(t){return h(t,this._cameraPositionInScaledSpace,this._distanceToLimbInScaledSpaceSquared)};var n=new b.Cartesian3;i.prototype.isScaledSpacePointVisiblePossiblyUnderEllipsoid=function(t,e){var i,r,a=this._ellipsoid;return i=y.defined(e)&&e<0&&a.minimumRadius>-e?((r=n).x=this._cameraPosition.x/(a.radii.x+e),r.y=this._cameraPosition.y/(a.radii.y+e),r.z=this._cameraPosition.z/(a.radii.z+e),r.x*r.x+r.y*r.y+r.z*r.z-1):(r=this._cameraPositionInScaledSpace,this._distanceToLimbInScaledSpaceSquared),h(t,r,i)},i.prototype.computeHorizonCullingPoint=function(t,e,i){return d(this._ellipsoid,t,e,i)};var o=b.Ellipsoid.clone(b.Ellipsoid.UNIT_SPHERE);i.prototype.computeHorizonCullingPointPossiblyUnderEllipsoid=function(t,e,i,r){return d(u(this._ellipsoid,i,o),t,e,r)},i.prototype.computeHorizonCullingPointFromVertices=function(t,e,i,r,a){return p(this._ellipsoid,t,e,i,r,a)},i.prototype.computeHorizonCullingPointFromVerticesPossiblyUnderEllipsoid=function(t,e,i,r,a,n){return p(u(this._ellipsoid,a,o),t,e,i,r,n)};var c=[];i.prototype.computeHorizonCullingPointFromRectangle=function(t,e,i){var r=b.Rectangle.subsample(t,e,0,c),a=v.BoundingSphere.fromPoints(r);if(!(b.Cartesian3.magnitude(a.center)<.1*e.minimumRadius))return this.computeHorizonCullingPoint(a.center,r,i)};var a=new b.Cartesian3;function u(t,e,i){if(y.defined(e)&&e<0&&t.minimumRadius>-e){var r=b.Cartesian3.fromElements(t.radii.x+e,t.radii.y+e,t.radii.z+e,a);t=b.Ellipsoid.fromCartesian3(r,i)}return t}function d(t,e,i,r){y.defined(r)||(r=new b.Cartesian3);for(var a=T(t,e),n=0,o=0,s=i.length;o<s;++o){var m=M(t,i[o],a);if(m<0)return;n=Math.max(n,m)}return g(a,n,r)}var l=new b.Cartesian3;function p(t,e,i,r,a,n){y.defined(n)||(n=new b.Cartesian3),r=y.defaultValue(r,3),a=y.defaultValue(a,b.Cartesian3.ZERO);for(var o=T(t,e),s=0,m=0,c=i.length;m<c;m+=r){l.x=i[m]+a.x,l.y=i[m+1]+a.y,l.z=i[m+2]+a.z;var u=M(t,l,o);if(u<0)return;s=Math.max(s,u)}return g(o,s,n)}function h(t,e,i){var r=e,a=i,n=b.Cartesian3.subtract(t,r,m),o=-b.Cartesian3.dot(n,r);return!(a<0?0<o:a<o&&o*o/b.Cartesian3.magnitudeSquared(n)>a)}var C=new b.Cartesian3,S=new b.Cartesian3;function M(t,e,i){var r=t.transformPositionToScaledSpace(e,C),a=b.Cartesian3.magnitudeSquared(r),n=Math.sqrt(a),o=b.Cartesian3.divideByScalar(r,n,S);a=Math.max(1,a);var s=1/(n=Math.max(1,n));return 1/(b.Cartesian3.dot(o,i)*s-b.Cartesian3.magnitude(b.Cartesian3.cross(o,i,o))*(Math.sqrt(a-1)*s))}function g(t,e,i){if(!(e<=0||e===1/0||e!=e))return b.Cartesian3.multiplyByScalar(t,e,i)}var r=new b.Cartesian3;function T(t,e){return b.Cartesian3.equals(e,b.Cartesian3.ZERO)?e:(t.transformPositionToScaledSpace(e,r),b.Cartesian3.normalize(r,r))}var P=Object.freeze({NONE:0,BITS12:1}),z=new b.Cartesian3,E=new b.Cartesian3,N=new b.Cartesian2,I=new v.Matrix4,B=new v.Matrix4,_=Math.pow(2,12);function w(t,e,i,r,a,n){var o,s,m,c=P.NONE;if(y.defined(t)&&y.defined(e)&&y.defined(i)&&y.defined(r)){var u=t.minimum,d=t.maximum,l=b.Cartesian3.subtract(d,u,E),p=i-e;c=Math.max(b.Cartesian3.maximumComponent(l),p)<_-1?P.BITS12:P.NONE,o=t.center,s=v.Matrix4.inverseTransformation(r,new v.Matrix4);var h=b.Cartesian3.negate(u,z);v.Matrix4.multiply(v.Matrix4.fromTranslation(h,I),s,s);var f=z;f.x=1/l.x,f.y=1/l.y,f.z=1/l.z,v.Matrix4.multiply(v.Matrix4.fromScale(f,I),s,s),m=v.Matrix4.clone(r),v.Matrix4.setTranslation(m,b.Cartesian3.ZERO,m),r=v.Matrix4.clone(r,new v.Matrix4);var x=v.Matrix4.fromTranslation(u,I),C=v.Matrix4.fromScale(l,B),S=v.Matrix4.multiply(x,C,I);v.Matrix4.multiply(r,S,r),v.Matrix4.multiply(m,S,m)}this.quantization=c,this.minimumHeight=e,this.maximumHeight=i,this.center=o,this.toScaledENU=s,this.fromScaledENU=r,this.matrix=m,this.hasVertexNormals=a,this.hasWebMercatorT=y.defaultValue(n,!1)}w.prototype.encode=function(t,e,i,r,a,n,o){var s=r.x,m=r.y;if(this.quantization===P.BITS12){(i=v.Matrix4.multiplyByPoint(this.toScaledENU,i,z)).x=f.CesiumMath.clamp(i.x,0,1),i.y=f.CesiumMath.clamp(i.y,0,1),i.z=f.CesiumMath.clamp(i.z,0,1);var c=this.maximumHeight-this.minimumHeight,u=f.CesiumMath.clamp((a-this.minimumHeight)/c,0,1);b.Cartesian2.fromElements(i.x,i.y,N);var d=x.AttributeCompression.compressTextureCoordinates(N);b.Cartesian2.fromElements(i.z,u,N);var l=x.AttributeCompression.compressTextureCoordinates(N);b.Cartesian2.fromElements(s,m,N);var p=x.AttributeCompression.compressTextureCoordinates(N);if(t[e++]=d,t[e++]=l,t[e++]=p,this.hasWebMercatorT){b.Cartesian2.fromElements(o,0,N);var h=x.AttributeCompression.compressTextureCoordinates(N);t[e++]=h}}else b.Cartesian3.subtract(i,this.center,z),t[e++]=z.x,t[e++]=z.y,t[e++]=z.z,t[e++]=a,t[e++]=s,t[e++]=m,this.hasWebMercatorT&&(t[e++]=o);return this.hasVertexNormals&&(t[e++]=x.AttributeCompression.octPackFloat(n)),e},w.prototype.decodePosition=function(t,e,i){if(y.defined(i)||(i=new b.Cartesian3),e*=this.getStride(),this.quantization!==P.BITS12)return i.x=t[e],i.y=t[e+1],i.z=t[e+2],b.Cartesian3.add(i,this.center,i);var r=x.AttributeCompression.decompressTextureCoordinates(t[e],N);i.x=r.x,i.y=r.y;var a=x.AttributeCompression.decompressTextureCoordinates(t[e+1],N);return i.z=a.x,v.Matrix4.multiplyByPoint(this.fromScaledENU,i,i)},w.prototype.decodeTextureCoordinates=function(t,e,i){return y.defined(i)||(i=new b.Cartesian2),e*=this.getStride(),this.quantization===P.BITS12?x.AttributeCompression.decompressTextureCoordinates(t[e+2],i):b.Cartesian2.fromElements(t[e+4],t[e+5],i)},w.prototype.decodeHeight=function(t,e){return e*=this.getStride(),this.quantization!==P.BITS12?t[e+3]:x.AttributeCompression.decompressTextureCoordinates(t[e+1],N).y*(this.maximumHeight-this.minimumHeight)+this.minimumHeight},w.prototype.decodeWebMercatorT=function(t,e){return e*=this.getStride(),this.quantization===P.BITS12?x.AttributeCompression.decompressTextureCoordinates(t[e+3],N).x:t[e+6]},w.prototype.getOctEncodedNormal=function(t,e,i){var r=t[e=(e+1)*this.getStride()-1]/256,a=Math.floor(r),n=256*(r-a);return b.Cartesian2.fromElements(a,n,i)},w.prototype.getStride=function(){var t;switch(this.quantization){case P.BITS12:t=3;break;default:t=6}return this.hasWebMercatorT&&++t,this.hasVertexNormals&&++t,t};var A={position3DAndHeight:0,textureCoordAndEncodedNormals:1},q={compressed0:0,compressed1:1};w.prototype.getAttributes=function(t){var e,i=s.ComponentDatatype.FLOAT,r=s.ComponentDatatype.getSizeInBytes(i);if(this.quantization===P.NONE){var a=2;return this.hasWebMercatorT&&++a,this.hasVertexNormals&&++a,[{index:A.position3DAndHeight,vertexBuffer:t,componentDatatype:i,componentsPerAttribute:4,offsetInBytes:0,strideInBytes:e=(4+a)*r},{index:A.textureCoordAndEncodedNormals,vertexBuffer:t,componentDatatype:i,componentsPerAttribute:a,offsetInBytes:4*r,strideInBytes:e}]}var n=3,o=0;return(this.hasWebMercatorT||this.hasVertexNormals)&&++n,this.hasWebMercatorT&&this.hasVertexNormals?[{index:q.compressed0,vertexBuffer:t,componentDatatype:i,componentsPerAttribute:n,offsetInBytes:0,strideInBytes:e=(n+ ++o)*r},{index:q.compressed1,vertexBuffer:t,componentDatatype:i,componentsPerAttribute:o,offsetInBytes:n*r,strideInBytes:e}]:[{index:q.compressed0,vertexBuffer:t,componentDatatype:i,componentsPerAttribute:n}]},w.prototype.getAttributeLocations=function(){return this.quantization===P.NONE?A:q},w.clone=function(t,e){return y.defined(e)||(e=new w),e.quantization=t.quantization,e.minimumHeight=t.minimumHeight,e.maximumHeight=t.maximumHeight,e.center=b.Cartesian3.clone(t.center),e.toScaledENU=v.Matrix4.clone(t.toScaledENU),e.fromScaledENU=v.Matrix4.clone(t.fromScaledENU),e.matrix=v.Matrix4.clone(t.matrix),e.hasVertexNormals=t.hasVertexNormals,e.hasWebMercatorT=t.hasWebMercatorT,e},t.EllipsoidalOccluder=i,t.TerrainEncoding=w});
