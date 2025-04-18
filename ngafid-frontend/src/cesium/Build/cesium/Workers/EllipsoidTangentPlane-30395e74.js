define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f"], function (e, p, n, x, s, o, a) {
    "use strict";

    function y(e, n, t) {
        this.minimum = x.Cartesian3.clone(p.defaultValue(e, x.Cartesian3.ZERO)), this.maximum = x.Cartesian3.clone(p.defaultValue(n, x.Cartesian3.ZERO)), t = p.defined(t) ? x.Cartesian3.clone(t) : x.Cartesian3.midpoint(this.minimum, this.maximum, new x.Cartesian3), this.center = t
    }

    y.fromPoints = function (e, n) {
        if (p.defined(n) || (n = new y), !p.defined(e) || 0 === e.length) return n.minimum = x.Cartesian3.clone(x.Cartesian3.ZERO, n.minimum), n.maximum = x.Cartesian3.clone(x.Cartesian3.ZERO, n.maximum), n.center = x.Cartesian3.clone(x.Cartesian3.ZERO, n.center), n;
        for (var t = e[0].x, i = e[0].y, a = e[0].z, r = e[0].x, s = e[0].y, o = e[0].z, m = e.length, l = 1; l < m; l++) {
            var c = e[l], u = c.x, d = c.y, f = c.z;
            t = Math.min(u, t), r = Math.max(u, r), i = Math.min(d, i), s = Math.max(d, s), a = Math.min(f, a), o = Math.max(f, o)
        }
        var h = n.minimum;
        h.x = t, h.y = i, h.z = a;
        var C = n.maximum;
        return C.x = r, C.y = s, C.z = o, n.center = x.Cartesian3.midpoint(h, C, n.center), n
    }, y.clone = function (e, n) {
        if (p.defined(e)) return p.defined(n) ? (n.minimum = x.Cartesian3.clone(e.minimum, n.minimum), n.maximum = x.Cartesian3.clone(e.maximum, n.maximum), n.center = x.Cartesian3.clone(e.center, n.center), n) : new y(e.minimum, e.maximum, e.center)
    }, y.equals = function (e, n) {
        return e === n || p.defined(e) && p.defined(n) && x.Cartesian3.equals(e.center, n.center) && x.Cartesian3.equals(e.minimum, n.minimum) && x.Cartesian3.equals(e.maximum, n.maximum)
    };
    var m = new x.Cartesian3;
    y.intersectPlane = function (e, n) {
        m = x.Cartesian3.subtract(e.maximum, e.minimum, m);
        var t = x.Cartesian3.multiplyByScalar(m, .5, m), i = n.normal,
            a = t.x * Math.abs(i.x) + t.y * Math.abs(i.y) + t.z * Math.abs(i.z),
            r = x.Cartesian3.dot(e.center, i) + n.distance;
        return 0 < r - a ? s.Intersect.INSIDE : r + a < 0 ? s.Intersect.OUTSIDE : s.Intersect.INTERSECTING
    }, y.prototype.clone = function (e) {
        return y.clone(this, e)
    }, y.prototype.intersectPlane = function (e) {
        return y.intersectPlane(this, e)
    }, y.prototype.equals = function (e) {
        return y.equals(this, e)
    };
    var r = new s.Cartesian4;

    function t(e, n) {
        e = (n = p.defaultValue(n, x.Ellipsoid.WGS84)).scaleToGeodeticSurface(e);
        var t = s.Transforms.eastNorthUpToFixedFrame(e, n);
        this._ellipsoid = n, this._origin = e, this._xAxis = x.Cartesian3.fromCartesian4(s.Matrix4.getColumn(t, 0, r)), this._yAxis = x.Cartesian3.fromCartesian4(s.Matrix4.getColumn(t, 1, r));
        var i = x.Cartesian3.fromCartesian4(s.Matrix4.getColumn(t, 2, r));
        this._plane = a.Plane.fromPointNormal(e, i)
    }

    Object.defineProperties(t.prototype, {
        ellipsoid: {
            get: function () {
                return this._ellipsoid
            }
        }, origin: {
            get: function () {
                return this._origin
            }
        }, plane: {
            get: function () {
                return this._plane
            }
        }, xAxis: {
            get: function () {
                return this._xAxis
            }
        }, yAxis: {
            get: function () {
                return this._yAxis
            }
        }, zAxis: {
            get: function () {
                return this._plane.normal
            }
        }
    });
    var i = new y;
    t.fromPoints = function (e, n) {
        return new t(y.fromPoints(e, i).center, n)
    };
    var l = new o.Ray, c = new x.Cartesian3;
    t.prototype.projectPointOntoPlane = function (e, n) {
        var t = l;
        t.origin = e, x.Cartesian3.normalize(e, t.direction);
        var i = o.IntersectionTests.rayPlane(t, this._plane, c);
        if (p.defined(i) || (x.Cartesian3.negate(t.direction, t.direction), i = o.IntersectionTests.rayPlane(t, this._plane, c)), p.defined(i)) {
            var a = x.Cartesian3.subtract(i, this._origin, i), r = x.Cartesian3.dot(this._xAxis, a),
                s = x.Cartesian3.dot(this._yAxis, a);
            return p.defined(n) ? (n.x = r, n.y = s, n) : new x.Cartesian2(r, s)
        }
    }, t.prototype.projectPointsOntoPlane = function (e, n) {
        p.defined(n) || (n = []);
        for (var t = 0, i = e.length, a = 0; a < i; a++) {
            var r = this.projectPointOntoPlane(e[a], n[t]);
            p.defined(r) && (n[t] = r, t++)
        }
        return n.length = t, n
    }, t.prototype.projectPointToNearestOnPlane = function (e, n) {
        p.defined(n) || (n = new x.Cartesian2);
        var t = l;
        t.origin = e, x.Cartesian3.clone(this._plane.normal, t.direction);
        var i = o.IntersectionTests.rayPlane(t, this._plane, c);
        p.defined(i) || (x.Cartesian3.negate(t.direction, t.direction), i = o.IntersectionTests.rayPlane(t, this._plane, c));
        var a = x.Cartesian3.subtract(i, this._origin, i), r = x.Cartesian3.dot(this._xAxis, a),
            s = x.Cartesian3.dot(this._yAxis, a);
        return n.x = r, n.y = s, n
    }, t.prototype.projectPointsToNearestOnPlane = function (e, n) {
        p.defined(n) || (n = []);
        var t = e.length;
        n.length = t;
        for (var i = 0; i < t; i++) n[i] = this.projectPointToNearestOnPlane(e[i], n[i]);
        return n
    };
    var u = new x.Cartesian3;
    t.prototype.projectPointOntoEllipsoid = function (e, n) {
        p.defined(n) || (n = new x.Cartesian3);
        var t = this._ellipsoid, i = this._origin, a = this._xAxis, r = this._yAxis, s = u;
        return x.Cartesian3.multiplyByScalar(a, e.x, s), n = x.Cartesian3.add(i, s, n), x.Cartesian3.multiplyByScalar(r, e.y, s), x.Cartesian3.add(n, s, n), t.scaleToGeocentricSurface(n, n), n
    }, t.prototype.projectPointsOntoEllipsoid = function (e, n) {
        var t = e.length;
        p.defined(n) ? n.length = t : n = new Array(t);
        for (var i = 0; i < t; ++i) n[i] = this.projectPointOntoEllipsoid(e[i], n[i]);
        return n
    }, e.AxisAlignedBoundingBox = y, e.EllipsoidTangentPlane = t
});
