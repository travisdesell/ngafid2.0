define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./WebGLConstants-4ae0db90"], function (e, a, t, O, v, n) {
    "use strict";
    var r = Object.freeze({NONE: 0, TRIANGLES: 1, LINES: 2, POLYLINES: 3});

    function M(e, t, n, r) {
        this[0] = a.defaultValue(e, 0), this[1] = a.defaultValue(n, 0), this[2] = a.defaultValue(t, 0), this[3] = a.defaultValue(r, 0)
    }

    M.packedLength = 4, M.pack = function (e, t, n) {
        return n = a.defaultValue(n, 0), t[n++] = e[0], t[n++] = e[1], t[n++] = e[2], t[n++] = e[3], t
    }, M.unpack = function (e, t, n) {
        return t = a.defaultValue(t, 0), a.defined(n) || (n = new M), n[0] = e[t++], n[1] = e[t++], n[2] = e[t++], n[3] = e[t++], n
    }, M.clone = function (e, t) {
        if (a.defined(e)) return a.defined(t) ? (t[0] = e[0], t[1] = e[1], t[2] = e[2], t[3] = e[3], t) : new M(e[0], e[2], e[1], e[3])
    }, M.fromArray = function (e, t, n) {
        return t = a.defaultValue(t, 0), a.defined(n) || (n = new M), n[0] = e[t], n[1] = e[t + 1], n[2] = e[t + 2], n[3] = e[t + 3], n
    }, M.fromColumnMajorArray = function (e, t) {
        return M.clone(e, t)
    }, M.fromRowMajorArray = function (e, t) {
        return a.defined(t) ? (t[0] = e[0], t[1] = e[2], t[2] = e[1], t[3] = e[3], t) : new M(e[0], e[1], e[2], e[3])
    }, M.fromScale = function (e, t) {
        return a.defined(t) ? (t[0] = e.x, t[1] = 0, t[2] = 0, t[3] = e.y, t) : new M(e.x, 0, 0, e.y)
    }, M.fromUniformScale = function (e, t) {
        return a.defined(t) ? (t[0] = e, t[1] = 0, t[2] = 0, t[3] = e, t) : new M(e, 0, 0, e)
    }, M.fromRotation = function (e, t) {
        var n = Math.cos(e), r = Math.sin(e);
        return a.defined(t) ? (t[0] = n, t[1] = r, t[2] = -r, t[3] = n, t) : new M(n, -r, r, n)
    }, M.toArray = function (e, t) {
        return a.defined(t) ? (t[0] = e[0], t[1] = e[1], t[2] = e[2], t[3] = e[3], t) : [e[0], e[1], e[2], e[3]]
    }, M.getElementIndex = function (e, t) {
        return 2 * e + t
    }, M.getColumn = function (e, t, n) {
        var r = 2 * t, a = e[r], i = e[1 + r];
        return n.x = a, n.y = i, n
    }, M.setColumn = function (e, t, n, r) {
        var a = 2 * t;
        return (r = M.clone(e, r))[a] = n.x, r[1 + a] = n.y, r
    }, M.getRow = function (e, t, n) {
        var r = e[t], a = e[t + 2];
        return n.x = r, n.y = a, n
    }, M.setRow = function (e, t, n, r) {
        return (r = M.clone(e, r))[t] = n.x, r[t + 2] = n.y, r
    };
    var i = new O.Cartesian2;
    M.getScale = function (e, t) {
        return t.x = O.Cartesian2.magnitude(O.Cartesian2.fromElements(e[0], e[1], i)), t.y = O.Cartesian2.magnitude(O.Cartesian2.fromElements(e[2], e[3], i)), t
    };
    var u = new O.Cartesian2;
    M.getMaximumScale = function (e) {
        return M.getScale(e, u), O.Cartesian2.maximumComponent(u)
    }, M.multiply = function (e, t, n) {
        var r = e[0] * t[0] + e[2] * t[1], a = e[0] * t[2] + e[2] * t[3], i = e[1] * t[0] + e[3] * t[1],
            u = e[1] * t[2] + e[3] * t[3];
        return n[0] = r, n[1] = i, n[2] = a, n[3] = u, n
    }, M.add = function (e, t, n) {
        return n[0] = e[0] + t[0], n[1] = e[1] + t[1], n[2] = e[2] + t[2], n[3] = e[3] + t[3], n
    }, M.subtract = function (e, t, n) {
        return n[0] = e[0] - t[0], n[1] = e[1] - t[1], n[2] = e[2] - t[2], n[3] = e[3] - t[3], n
    }, M.multiplyByVector = function (e, t, n) {
        var r = e[0] * t.x + e[2] * t.y, a = e[1] * t.x + e[3] * t.y;
        return n.x = r, n.y = a, n
    }, M.multiplyByScalar = function (e, t, n) {
        return n[0] = e[0] * t, n[1] = e[1] * t, n[2] = e[2] * t, n[3] = e[3] * t, n
    }, M.multiplyByScale = function (e, t, n) {
        return n[0] = e[0] * t.x, n[1] = e[1] * t.x, n[2] = e[2] * t.y, n[3] = e[3] * t.y, n
    }, M.negate = function (e, t) {
        return t[0] = -e[0], t[1] = -e[1], t[2] = -e[2], t[3] = -e[3], t
    }, M.transpose = function (e, t) {
        var n = e[0], r = e[2], a = e[1], i = e[3];
        return t[0] = n, t[1] = r, t[2] = a, t[3] = i, t
    }, M.abs = function (e, t) {
        return t[0] = Math.abs(e[0]), t[1] = Math.abs(e[1]), t[2] = Math.abs(e[2]), t[3] = Math.abs(e[3]), t
    }, M.equals = function (e, t) {
        return e === t || a.defined(e) && a.defined(t) && e[0] === t[0] && e[1] === t[1] && e[2] === t[2] && e[3] === t[3]
    }, M.equalsArray = function (e, t, n) {
        return e[0] === t[n] && e[1] === t[n + 1] && e[2] === t[n + 2] && e[3] === t[n + 3]
    }, M.equalsEpsilon = function (e, t, n) {
        return e === t || a.defined(e) && a.defined(t) && Math.abs(e[0] - t[0]) <= n && Math.abs(e[1] - t[1]) <= n && Math.abs(e[2] - t[2]) <= n && Math.abs(e[3] - t[3]) <= n
    }, M.IDENTITY = Object.freeze(new M(1, 0, 0, 1)), M.ZERO = Object.freeze(new M(0, 0, 0, 0)), M.COLUMN0ROW0 = 0, M.COLUMN0ROW1 = 1, M.COLUMN1ROW0 = 2, M.COLUMN1ROW1 = 3, Object.defineProperties(M.prototype, {
        length: {
            get: function () {
                return M.packedLength
            }
        }
    }), M.prototype.clone = function (e) {
        return M.clone(this, e)
    }, M.prototype.equals = function (e) {
        return M.equals(this, e)
    }, M.prototype.equalsEpsilon = function (e, t) {
        return M.equalsEpsilon(this, e, t)
    }, M.prototype.toString = function () {
        return "(" + this[0] + ", " + this[2] + ")\n(" + this[1] + ", " + this[3] + ")"
    };
    var o = {
        POINTS: n.WebGLConstants.POINTS,
        LINES: n.WebGLConstants.LINES,
        LINE_LOOP: n.WebGLConstants.LINE_LOOP,
        LINE_STRIP: n.WebGLConstants.LINE_STRIP,
        TRIANGLES: n.WebGLConstants.TRIANGLES,
        TRIANGLE_STRIP: n.WebGLConstants.TRIANGLE_STRIP,
        TRIANGLE_FAN: n.WebGLConstants.TRIANGLE_FAN,
        validate: function (e) {
            return e === o.POINTS || e === o.LINES || e === o.LINE_LOOP || e === o.LINE_STRIP || e === o.TRIANGLES || e === o.TRIANGLE_STRIP || e === o.TRIANGLE_FAN
        }
    }, s = Object.freeze(o);

    function f(e) {
        e = a.defaultValue(e, a.defaultValue.EMPTY_OBJECT), this.attributes = e.attributes, this.indices = e.indices, this.primitiveType = a.defaultValue(e.primitiveType, s.TRIANGLES), this.boundingSphere = e.boundingSphere, this.geometryType = a.defaultValue(e.geometryType, r.NONE), this.boundingSphereCV = e.boundingSphereCV, this.offsetAttribute = e.offsetAttribute
    }

    f.computeNumberOfVertices = function (e) {
        var t = -1;
        for (var n in e.attributes) if (e.attributes.hasOwnProperty(n) && a.defined(e.attributes[n]) && a.defined(e.attributes[n].values)) {
            var r = e.attributes[n];
            t = r.values.length / r.componentsPerAttribute
        }
        return t
    };
    var R = new O.Cartographic, P = new O.Cartesian3, V = new v.Matrix4,
        G = [new O.Cartographic, new O.Cartographic, new O.Cartographic],
        _ = [new O.Cartesian2, new O.Cartesian2, new O.Cartesian2],
        W = [new O.Cartesian2, new O.Cartesian2, new O.Cartesian2], B = new O.Cartesian3, F = new v.Quaternion,
        k = new v.Matrix4, Y = new M;
    f._textureCoordinateRotationPoints = function (e, t, n, r) {
        var a, i = O.Rectangle.center(r, R), u = O.Cartographic.toCartesian(i, n, P),
            o = v.Transforms.eastNorthUpToFixedFrame(u, n, V), s = v.Matrix4.inverse(o, V), f = _, c = G;
        c[0].longitude = r.west, c[0].latitude = r.south, c[1].longitude = r.west, c[1].latitude = r.north, c[2].longitude = r.east, c[2].latitude = r.south;
        var l = B;
        for (a = 0; a < 3; a++) O.Cartographic.toCartesian(c[a], n, l), l = v.Matrix4.multiplyByPointAsVector(s, l, l), f[a].x = l.x, f[a].y = l.y;
        var d = v.Quaternion.fromAxisAngle(O.Cartesian3.UNIT_Z, -t, F), y = v.Matrix3.fromQuaternion(d, k),
            m = e.length, p = Number.POSITIVE_INFINITY, h = Number.POSITIVE_INFINITY, N = Number.NEGATIVE_INFINITY,
            I = Number.NEGATIVE_INFINITY;
        for (a = 0; a < m; a++) l = v.Matrix4.multiplyByPointAsVector(s, e[a], l), l = v.Matrix3.multiplyByVector(y, l, l), p = Math.min(p, l.x), h = Math.min(h, l.y), N = Math.max(N, l.x), I = Math.max(I, l.y);
        var C = M.fromRotation(t, Y), b = W;
        b[0].x = p, b[0].y = h, b[1].x = p, b[1].y = I, b[2].x = N, b[2].y = h;
        var T = f[0], E = f[2].x - T.x, x = f[1].y - T.y;
        for (a = 0; a < 3; a++) {
            var L = b[a];
            M.multiplyByVector(C, L, L), L.x = (L.x - T.x) / E, L.y = (L.y - T.y) / x
        }
        var w = b[0], g = b[1], S = b[2], A = new Array(6);
        return O.Cartesian2.pack(w, A), O.Cartesian2.pack(g, A, 2), O.Cartesian2.pack(S, A, 4), A
    }, e.Geometry = f, e.GeometryAttribute = function (e) {
        e = a.defaultValue(e, a.defaultValue.EMPTY_OBJECT), this.componentDatatype = e.componentDatatype, this.componentsPerAttribute = e.componentsPerAttribute, this.normalize = a.defaultValue(e.normalize, !1), this.values = e.values
    }, e.GeometryType = r, e.Matrix2 = M, e.PrimitiveType = s
});
