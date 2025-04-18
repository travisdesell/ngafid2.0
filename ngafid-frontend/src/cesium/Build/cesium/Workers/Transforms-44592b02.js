define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./RuntimeError-6122571f"], function (e, I, i, X, P, H) {
    "use strict";

    function t(e) {
        this._ellipsoid = I.defaultValue(e, P.Ellipsoid.WGS84), this._semimajorAxis = this._ellipsoid.maximumRadius, this._oneOverSemimajorAxis = 1 / this._semimajorAxis
    }

    Object.defineProperties(t.prototype, {
        ellipsoid: {
            get: function () {
                return this._ellipsoid
            }
        }
    }), t.prototype.project = function (e, t) {
        var n = this._semimajorAxis, r = e.longitude * n, a = e.latitude * n, i = e.height;
        return I.defined(t) ? (t.x = r, t.y = a, t.z = i, t) : new P.Cartesian3(r, a, i)
    }, t.prototype.unproject = function (e, t) {
        var n = this._oneOverSemimajorAxis, r = e.x * n, a = e.y * n, i = e.z;
        return I.defined(t) ? (t.longitude = r, t.latitude = a, t.height = i, t) : new P.Cartographic(r, a, i)
    };
    var s = Object.freeze({OUTSIDE: -1, INTERSECTING: 0, INSIDE: 1});

    function o(e, t) {
        this.start = I.defaultValue(e, 0), this.stop = I.defaultValue(t, 0)
    }

    function J(e, t, n, r, a, i, s, o, u) {
        this[0] = I.defaultValue(e, 0), this[1] = I.defaultValue(r, 0), this[2] = I.defaultValue(s, 0), this[3] = I.defaultValue(t, 0), this[4] = I.defaultValue(a, 0), this[5] = I.defaultValue(o, 0), this[6] = I.defaultValue(n, 0), this[7] = I.defaultValue(i, 0), this[8] = I.defaultValue(u, 0)
    }

    J.packedLength = 9, J.pack = function (e, t, n) {
        return n = I.defaultValue(n, 0), t[n++] = e[0], t[n++] = e[1], t[n++] = e[2], t[n++] = e[3], t[n++] = e[4], t[n++] = e[5], t[n++] = e[6], t[n++] = e[7], t[n++] = e[8], t
    }, J.unpack = function (e, t, n) {
        return t = I.defaultValue(t, 0), I.defined(n) || (n = new J), n[0] = e[t++], n[1] = e[t++], n[2] = e[t++], n[3] = e[t++], n[4] = e[t++], n[5] = e[t++], n[6] = e[t++], n[7] = e[t++], n[8] = e[t++], n
    }, J.clone = function (e, t) {
        if (I.defined(e)) return I.defined(t) ? (t[0] = e[0], t[1] = e[1], t[2] = e[2], t[3] = e[3], t[4] = e[4], t[5] = e[5], t[6] = e[6], t[7] = e[7], t[8] = e[8], t) : new J(e[0], e[3], e[6], e[1], e[4], e[7], e[2], e[5], e[8])
    }, J.fromArray = function (e, t, n) {
        return t = I.defaultValue(t, 0), I.defined(n) || (n = new J), n[0] = e[t], n[1] = e[t + 1], n[2] = e[t + 2], n[3] = e[t + 3], n[4] = e[t + 4], n[5] = e[t + 5], n[6] = e[t + 6], n[7] = e[t + 7], n[8] = e[t + 8], n
    }, J.fromColumnMajorArray = function (e, t) {
        return J.clone(e, t)
    }, J.fromRowMajorArray = function (e, t) {
        return I.defined(t) ? (t[0] = e[0], t[1] = e[3], t[2] = e[6], t[3] = e[1], t[4] = e[4], t[5] = e[7], t[6] = e[2], t[7] = e[5], t[8] = e[8], t) : new J(e[0], e[1], e[2], e[3], e[4], e[5], e[6], e[7], e[8])
    }, J.fromQuaternion = function (e, t) {
        var n = e.x * e.x, r = e.x * e.y, a = e.x * e.z, i = e.x * e.w, s = e.y * e.y, o = e.y * e.z, u = e.y * e.w,
            l = e.z * e.z, c = e.z * e.w, d = e.w * e.w, f = n - s - l + d, h = 2 * (r - c), p = 2 * (a + u),
            m = 2 * (r + c), y = s - n - l + d, v = 2 * (o - i), C = 2 * (a - u), w = 2 * (o + i), g = -n - s + l + d;
        return I.defined(t) ? (t[0] = f, t[1] = m, t[2] = C, t[3] = h, t[4] = y, t[5] = w, t[6] = p, t[7] = v, t[8] = g, t) : new J(f, h, p, m, y, v, C, w, g)
    }, J.fromHeadingPitchRoll = function (e, t) {
        var n = Math.cos(-e.pitch), r = Math.cos(-e.heading), a = Math.cos(e.roll), i = Math.sin(-e.pitch),
            s = Math.sin(-e.heading), o = Math.sin(e.roll), u = n * r, l = -a * s + o * i * r, c = o * s + a * i * r,
            d = n * s, f = a * r + o * i * s, h = -o * r + a * i * s, p = -i, m = o * n, y = a * n;
        return I.defined(t) ? (t[0] = u, t[1] = d, t[2] = p, t[3] = l, t[4] = f, t[5] = m, t[6] = c, t[7] = h, t[8] = y, t) : new J(u, l, c, d, f, h, p, m, y)
    }, J.fromScale = function (e, t) {
        return I.defined(t) ? (t[0] = e.x, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = e.y, t[5] = 0, t[6] = 0, t[7] = 0, t[8] = e.z, t) : new J(e.x, 0, 0, 0, e.y, 0, 0, 0, e.z)
    }, J.fromUniformScale = function (e, t) {
        return I.defined(t) ? (t[0] = e, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = e, t[5] = 0, t[6] = 0, t[7] = 0, t[8] = e, t) : new J(e, 0, 0, 0, e, 0, 0, 0, e)
    }, J.fromCrossProduct = function (e, t) {
        return I.defined(t) ? (t[0] = 0, t[1] = e.z, t[2] = -e.y, t[3] = -e.z, t[4] = 0, t[5] = e.x, t[6] = e.y, t[7] = -e.x, t[8] = 0, t) : new J(0, -e.z, e.y, e.z, 0, -e.x, -e.y, e.x, 0)
    }, J.fromRotationX = function (e, t) {
        var n = Math.cos(e), r = Math.sin(e);
        return I.defined(t) ? (t[0] = 1, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = n, t[5] = r, t[6] = 0, t[7] = -r, t[8] = n, t) : new J(1, 0, 0, 0, n, -r, 0, r, n)
    }, J.fromRotationY = function (e, t) {
        var n = Math.cos(e), r = Math.sin(e);
        return I.defined(t) ? (t[0] = n, t[1] = 0, t[2] = -r, t[3] = 0, t[4] = 1, t[5] = 0, t[6] = r, t[7] = 0, t[8] = n, t) : new J(n, 0, r, 0, 1, 0, -r, 0, n)
    }, J.fromRotationZ = function (e, t) {
        var n = Math.cos(e), r = Math.sin(e);
        return I.defined(t) ? (t[0] = n, t[1] = r, t[2] = 0, t[3] = -r, t[4] = n, t[5] = 0, t[6] = 0, t[7] = 0, t[8] = 1, t) : new J(n, -r, 0, r, n, 0, 0, 0, 1)
    }, J.toArray = function (e, t) {
        return I.defined(t) ? (t[0] = e[0], t[1] = e[1], t[2] = e[2], t[3] = e[3], t[4] = e[4], t[5] = e[5], t[6] = e[6], t[7] = e[7], t[8] = e[8], t) : [e[0], e[1], e[2], e[3], e[4], e[5], e[6], e[7], e[8]]
    }, J.getElementIndex = function (e, t) {
        return 3 * e + t
    }, J.getColumn = function (e, t, n) {
        var r = 3 * t, a = e[r], i = e[1 + r], s = e[2 + r];
        return n.x = a, n.y = i, n.z = s, n
    }, J.setColumn = function (e, t, n, r) {
        var a = 3 * t;
        return (r = J.clone(e, r))[a] = n.x, r[1 + a] = n.y, r[2 + a] = n.z, r
    }, J.getRow = function (e, t, n) {
        var r = e[t], a = e[t + 3], i = e[t + 6];
        return n.x = r, n.y = a, n.z = i, n
    }, J.setRow = function (e, t, n, r) {
        return (r = J.clone(e, r))[t] = n.x, r[t + 3] = n.y, r[t + 6] = n.z, r
    };
    var n = new P.Cartesian3;
    J.getScale = function (e, t) {
        return t.x = P.Cartesian3.magnitude(P.Cartesian3.fromElements(e[0], e[1], e[2], n)), t.y = P.Cartesian3.magnitude(P.Cartesian3.fromElements(e[3], e[4], e[5], n)), t.z = P.Cartesian3.magnitude(P.Cartesian3.fromElements(e[6], e[7], e[8], n)), t
    };
    var r = new P.Cartesian3;
    J.getMaximumScale = function (e) {
        return J.getScale(e, r), P.Cartesian3.maximumComponent(r)
    }, J.multiply = function (e, t, n) {
        var r = e[0] * t[0] + e[3] * t[1] + e[6] * t[2], a = e[1] * t[0] + e[4] * t[1] + e[7] * t[2],
            i = e[2] * t[0] + e[5] * t[1] + e[8] * t[2], s = e[0] * t[3] + e[3] * t[4] + e[6] * t[5],
            o = e[1] * t[3] + e[4] * t[4] + e[7] * t[5], u = e[2] * t[3] + e[5] * t[4] + e[8] * t[5],
            l = e[0] * t[6] + e[3] * t[7] + e[6] * t[8], c = e[1] * t[6] + e[4] * t[7] + e[7] * t[8],
            d = e[2] * t[6] + e[5] * t[7] + e[8] * t[8];
        return n[0] = r, n[1] = a, n[2] = i, n[3] = s, n[4] = o, n[5] = u, n[6] = l, n[7] = c, n[8] = d, n
    }, J.add = function (e, t, n) {
        return n[0] = e[0] + t[0], n[1] = e[1] + t[1], n[2] = e[2] + t[2], n[3] = e[3] + t[3], n[4] = e[4] + t[4], n[5] = e[5] + t[5], n[6] = e[6] + t[6], n[7] = e[7] + t[7], n[8] = e[8] + t[8], n
    }, J.subtract = function (e, t, n) {
        return n[0] = e[0] - t[0], n[1] = e[1] - t[1], n[2] = e[2] - t[2], n[3] = e[3] - t[3], n[4] = e[4] - t[4], n[5] = e[5] - t[5], n[6] = e[6] - t[6], n[7] = e[7] - t[7], n[8] = e[8] - t[8], n
    }, J.multiplyByVector = function (e, t, n) {
        var r = t.x, a = t.y, i = t.z, s = e[0] * r + e[3] * a + e[6] * i, o = e[1] * r + e[4] * a + e[7] * i,
            u = e[2] * r + e[5] * a + e[8] * i;
        return n.x = s, n.y = o, n.z = u, n
    }, J.multiplyByScalar = function (e, t, n) {
        return n[0] = e[0] * t, n[1] = e[1] * t, n[2] = e[2] * t, n[3] = e[3] * t, n[4] = e[4] * t, n[5] = e[5] * t, n[6] = e[6] * t, n[7] = e[7] * t, n[8] = e[8] * t, n
    }, J.multiplyByScale = function (e, t, n) {
        return n[0] = e[0] * t.x, n[1] = e[1] * t.x, n[2] = e[2] * t.x, n[3] = e[3] * t.y, n[4] = e[4] * t.y, n[5] = e[5] * t.y, n[6] = e[6] * t.z, n[7] = e[7] * t.z, n[8] = e[8] * t.z, n
    }, J.negate = function (e, t) {
        return t[0] = -e[0], t[1] = -e[1], t[2] = -e[2], t[3] = -e[3], t[4] = -e[4], t[5] = -e[5], t[6] = -e[6], t[7] = -e[7], t[8] = -e[8], t
    }, J.transpose = function (e, t) {
        var n = e[0], r = e[3], a = e[6], i = e[1], s = e[4], o = e[7], u = e[2], l = e[5], c = e[8];
        return t[0] = n, t[1] = r, t[2] = a, t[3] = i, t[4] = s, t[5] = o, t[6] = u, t[7] = l, t[8] = c, t
    };
    var a = new P.Cartesian3(1, 1, 1);
    J.getRotation = function (e, t) {
        var n = P.Cartesian3.divideComponents(a, J.getScale(e, r), r);
        return t = J.multiplyByScale(e, n, t)
    };
    var h = [1, 0, 0], p = [2, 2, 1];

    function u(e) {
        for (var t = 0, n = 0; n < 3; ++n) {
            var r = e[J.getElementIndex(p[n], h[n])];
            t += 2 * r * r
        }
        return Math.sqrt(t)
    }

    function l(e, t) {
        for (var n = X.CesiumMath.EPSILON15, r = 0, a = 1, i = 0; i < 3; ++i) {
            var s = Math.abs(e[J.getElementIndex(p[i], h[i])]);
            r < s && (a = i, r = s)
        }
        var o = 1, u = 0, l = h[a], c = p[a];
        if (Math.abs(e[J.getElementIndex(c, l)]) > n) {
            var d, f = (e[J.getElementIndex(c, c)] - e[J.getElementIndex(l, l)]) / 2 / e[J.getElementIndex(c, l)];
            u = (d = f < 0 ? -1 / (-f + Math.sqrt(1 + f * f)) : 1 / (f + Math.sqrt(1 + f * f))) * (o = 1 / Math.sqrt(1 + d * d))
        }
        return (t = J.clone(J.IDENTITY, t))[J.getElementIndex(l, l)] = t[J.getElementIndex(c, c)] = o, t[J.getElementIndex(c, l)] = u, t[J.getElementIndex(l, c)] = -u, t
    }

    var c = new J, d = new J;

    function G(e, t, n, r) {
        this.x = I.defaultValue(e, 0), this.y = I.defaultValue(t, 0), this.z = I.defaultValue(n, 0), this.w = I.defaultValue(r, 0)
    }

    J.computeEigenDecomposition = function (e, t) {
        var n = X.CesiumMath.EPSILON20, r = 0, a = 0;
        I.defined(t) || (t = {});
        for (var i = t.unitary = J.clone(J.IDENTITY, t.unitary), s = t.diagonal = J.clone(e, t.diagonal), o = n * function (e) {
            for (var t = 0, n = 0; n < 9; ++n) {
                var r = e[n];
                t += r * r
            }
            return Math.sqrt(t)
        }(s); a < 10 && u(s) > o;) l(s, c), J.transpose(c, d), J.multiply(s, c, s), J.multiply(d, s, s), J.multiply(i, c, i), 2 < ++r && (++a, r = 0);
        return t
    }, J.abs = function (e, t) {
        return t[0] = Math.abs(e[0]), t[1] = Math.abs(e[1]), t[2] = Math.abs(e[2]), t[3] = Math.abs(e[3]), t[4] = Math.abs(e[4]), t[5] = Math.abs(e[5]), t[6] = Math.abs(e[6]), t[7] = Math.abs(e[7]), t[8] = Math.abs(e[8]), t
    }, J.determinant = function (e) {
        var t = e[0], n = e[3], r = e[6], a = e[1], i = e[4], s = e[7], o = e[2], u = e[5], l = e[8];
        return t * (i * l - u * s) + a * (u * r - n * l) + o * (n * s - i * r)
    }, J.inverse = function (e, t) {
        var n = e[0], r = e[1], a = e[2], i = e[3], s = e[4], o = e[5], u = e[6], l = e[7], c = e[8],
            d = J.determinant(e);
        return t[0] = s * c - l * o, t[1] = l * a - r * c, t[2] = r * o - s * a, t[3] = u * o - i * c, t[4] = n * c - u * a, t[5] = i * a - n * o, t[6] = i * l - u * s, t[7] = u * r - n * l, t[8] = n * s - i * r, J.multiplyByScalar(t, 1 / d, t)
    }, J.equals = function (e, t) {
        return e === t || I.defined(e) && I.defined(t) && e[0] === t[0] && e[1] === t[1] && e[2] === t[2] && e[3] === t[3] && e[4] === t[4] && e[5] === t[5] && e[6] === t[6] && e[7] === t[7] && e[8] === t[8]
    }, J.equalsEpsilon = function (e, t, n) {
        return e === t || I.defined(e) && I.defined(t) && Math.abs(e[0] - t[0]) <= n && Math.abs(e[1] - t[1]) <= n && Math.abs(e[2] - t[2]) <= n && Math.abs(e[3] - t[3]) <= n && Math.abs(e[4] - t[4]) <= n && Math.abs(e[5] - t[5]) <= n && Math.abs(e[6] - t[6]) <= n && Math.abs(e[7] - t[7]) <= n && Math.abs(e[8] - t[8]) <= n
    }, J.IDENTITY = Object.freeze(new J(1, 0, 0, 0, 1, 0, 0, 0, 1)), J.ZERO = Object.freeze(new J(0, 0, 0, 0, 0, 0, 0, 0, 0)), J.COLUMN0ROW0 = 0, J.COLUMN0ROW1 = 1, J.COLUMN0ROW2 = 2, J.COLUMN1ROW0 = 3, J.COLUMN1ROW1 = 4, J.COLUMN1ROW2 = 5, J.COLUMN2ROW0 = 6, J.COLUMN2ROW1 = 7, J.COLUMN2ROW2 = 8, Object.defineProperties(J.prototype, {
        length: {
            get: function () {
                return J.packedLength
            }
        }
    }), J.prototype.clone = function (e) {
        return J.clone(this, e)
    }, J.prototype.equals = function (e) {
        return J.equals(this, e)
    }, J.equalsArray = function (e, t, n) {
        return e[0] === t[n] && e[1] === t[n + 1] && e[2] === t[n + 2] && e[3] === t[n + 3] && e[4] === t[n + 4] && e[5] === t[n + 5] && e[6] === t[n + 6] && e[7] === t[n + 7] && e[8] === t[n + 8]
    }, J.prototype.equalsEpsilon = function (e, t) {
        return J.equalsEpsilon(this, e, t)
    }, J.prototype.toString = function () {
        return "(" + this[0] + ", " + this[3] + ", " + this[6] + ")\n(" + this[1] + ", " + this[4] + ", " + this[7] + ")\n(" + this[2] + ", " + this[5] + ", " + this[8] + ")"
    }, G.fromElements = function (e, t, n, r, a) {
        return I.defined(a) ? (a.x = e, a.y = t, a.z = n, a.w = r, a) : new G(e, t, n, r)
    }, G.fromColor = function (e, t) {
        return I.defined(t) ? (t.x = e.red, t.y = e.green, t.z = e.blue, t.w = e.alpha, t) : new G(e.red, e.green, e.blue, e.alpha)
    }, G.clone = function (e, t) {
        if (I.defined(e)) return I.defined(t) ? (t.x = e.x, t.y = e.y, t.z = e.z, t.w = e.w, t) : new G(e.x, e.y, e.z, e.w)
    }, G.packedLength = 4, G.pack = function (e, t, n) {
        return n = I.defaultValue(n, 0), t[n++] = e.x, t[n++] = e.y, t[n++] = e.z, t[n] = e.w, t
    }, G.unpack = function (e, t, n) {
        return t = I.defaultValue(t, 0), I.defined(n) || (n = new G), n.x = e[t++], n.y = e[t++], n.z = e[t++], n.w = e[t], n
    }, G.packArray = function (e, t) {
        var n = e.length, r = 4 * n;
        if (I.defined(t)) {
            if (!Array.isArray(t) && t.length !== r) throw new i.DeveloperError("If result is a typed array, it must have exactly array.length * 4 elements");
            t.length !== r && (t.length = r)
        } else t = new Array(r);
        for (var a = 0; a < n; ++a) G.pack(e[a], t, 4 * a);
        return t
    }, G.unpackArray = function (e, t) {
        var n = e.length;
        I.defined(t) ? t.length = n / 4 : t = new Array(n / 4);
        for (var r = 0; r < n; r += 4) {
            var a = r / 4;
            t[a] = G.unpack(e, r, t[a])
        }
        return t
    }, G.fromArray = G.unpack, G.maximumComponent = function (e) {
        return Math.max(e.x, e.y, e.z, e.w)
    }, G.minimumComponent = function (e) {
        return Math.min(e.x, e.y, e.z, e.w)
    }, G.minimumByComponent = function (e, t, n) {
        return n.x = Math.min(e.x, t.x), n.y = Math.min(e.y, t.y), n.z = Math.min(e.z, t.z), n.w = Math.min(e.w, t.w), n
    }, G.maximumByComponent = function (e, t, n) {
        return n.x = Math.max(e.x, t.x), n.y = Math.max(e.y, t.y), n.z = Math.max(e.z, t.z), n.w = Math.max(e.w, t.w), n
    }, G.magnitudeSquared = function (e) {
        return e.x * e.x + e.y * e.y + e.z * e.z + e.w * e.w
    }, G.magnitude = function (e) {
        return Math.sqrt(G.magnitudeSquared(e))
    };
    var f = new G;
    G.distance = function (e, t) {
        return G.subtract(e, t, f), G.magnitude(f)
    }, G.distanceSquared = function (e, t) {
        return G.subtract(e, t, f), G.magnitudeSquared(f)
    }, G.normalize = function (e, t) {
        var n = G.magnitude(e);
        return t.x = e.x / n, t.y = e.y / n, t.z = e.z / n, t.w = e.w / n, t
    }, G.dot = function (e, t) {
        return e.x * t.x + e.y * t.y + e.z * t.z + e.w * t.w
    }, G.multiplyComponents = function (e, t, n) {
        return n.x = e.x * t.x, n.y = e.y * t.y, n.z = e.z * t.z, n.w = e.w * t.w, n
    }, G.divideComponents = function (e, t, n) {
        return n.x = e.x / t.x, n.y = e.y / t.y, n.z = e.z / t.z, n.w = e.w / t.w, n
    }, G.add = function (e, t, n) {
        return n.x = e.x + t.x, n.y = e.y + t.y, n.z = e.z + t.z, n.w = e.w + t.w, n
    }, G.subtract = function (e, t, n) {
        return n.x = e.x - t.x, n.y = e.y - t.y, n.z = e.z - t.z, n.w = e.w - t.w, n
    }, G.multiplyByScalar = function (e, t, n) {
        return n.x = e.x * t, n.y = e.y * t, n.z = e.z * t, n.w = e.w * t, n
    }, G.divideByScalar = function (e, t, n) {
        return n.x = e.x / t, n.y = e.y / t, n.z = e.z / t, n.w = e.w / t, n
    }, G.negate = function (e, t) {
        return t.x = -e.x, t.y = -e.y, t.z = -e.z, t.w = -e.w, t
    }, G.abs = function (e, t) {
        return t.x = Math.abs(e.x), t.y = Math.abs(e.y), t.z = Math.abs(e.z), t.w = Math.abs(e.w), t
    };
    var m = new G;
    G.lerp = function (e, t, n, r) {
        return G.multiplyByScalar(t, n, m), r = G.multiplyByScalar(e, 1 - n, r), G.add(m, r, r)
    };
    var y = new G;
    G.mostOrthogonalAxis = function (e, t) {
        var n = G.normalize(e, y);
        return G.abs(n, n), t = n.x <= n.y ? n.x <= n.z ? n.x <= n.w ? G.clone(G.UNIT_X, t) : G.clone(G.UNIT_W, t) : n.z <= n.w ? G.clone(G.UNIT_Z, t) : G.clone(G.UNIT_W, t) : n.y <= n.z ? n.y <= n.w ? G.clone(G.UNIT_Y, t) : G.clone(G.UNIT_W, t) : n.z <= n.w ? G.clone(G.UNIT_Z, t) : G.clone(G.UNIT_W, t)
    }, G.equals = function (e, t) {
        return e === t || I.defined(e) && I.defined(t) && e.x === t.x && e.y === t.y && e.z === t.z && e.w === t.w
    }, G.equalsArray = function (e, t, n) {
        return e.x === t[n] && e.y === t[n + 1] && e.z === t[n + 2] && e.w === t[n + 3]
    }, G.equalsEpsilon = function (e, t, n, r) {
        return e === t || I.defined(e) && I.defined(t) && X.CesiumMath.equalsEpsilon(e.x, t.x, n, r) && X.CesiumMath.equalsEpsilon(e.y, t.y, n, r) && X.CesiumMath.equalsEpsilon(e.z, t.z, n, r) && X.CesiumMath.equalsEpsilon(e.w, t.w, n, r)
    }, G.ZERO = Object.freeze(new G(0, 0, 0, 0)), G.UNIT_X = Object.freeze(new G(1, 0, 0, 0)), G.UNIT_Y = Object.freeze(new G(0, 1, 0, 0)), G.UNIT_Z = Object.freeze(new G(0, 0, 1, 0)), G.UNIT_W = Object.freeze(new G(0, 0, 0, 1)), G.prototype.clone = function (e) {
        return G.clone(this, e)
    }, G.prototype.equals = function (e) {
        return G.equals(this, e)
    }, G.prototype.equalsEpsilon = function (e, t, n) {
        return G.equalsEpsilon(this, e, t, n)
    }, G.prototype.toString = function () {
        return "(" + this.x + ", " + this.y + ", " + this.z + ", " + this.w + ")"
    };
    var v = new Float32Array(1), C = 256;

    function Q(e, t, n, r, a, i, s, o, u, l, c, d, f, h, p, m) {
        this[0] = I.defaultValue(e, 0), this[1] = I.defaultValue(a, 0), this[2] = I.defaultValue(u, 0), this[3] = I.defaultValue(f, 0), this[4] = I.defaultValue(t, 0), this[5] = I.defaultValue(i, 0), this[6] = I.defaultValue(l, 0), this[7] = I.defaultValue(h, 0), this[8] = I.defaultValue(n, 0), this[9] = I.defaultValue(s, 0), this[10] = I.defaultValue(c, 0), this[11] = I.defaultValue(p, 0), this[12] = I.defaultValue(r, 0), this[13] = I.defaultValue(o, 0), this[14] = I.defaultValue(d, 0), this[15] = I.defaultValue(m, 0)
    }

    G.packFloat = function (e, t) {
        if (I.defined(t) || (t = new G), v[0] = e, 0 === (e = v[0])) return G.clone(G.ZERO, t);
        var n, r = e < 0 ? 1 : 0;
        isFinite(e) ? (e = Math.abs(e), n = Math.floor(X.CesiumMath.logBase(e, 10)) + 1, e /= Math.pow(10, n)) : (e = .1, n = 38);
        var a = e * C;
        return t.x = Math.floor(a), a = (a - t.x) * C, t.y = Math.floor(a), a = (a - t.y) * C, t.z = Math.floor(a), t.w = 2 * (n + 38) + r, t
    }, G.unpackFloat = function (e) {
        var t = e.w / 2, n = Math.floor(t), r = 2 * (t - n);
        if (r = -(r = 2 * r - 1), 38 <= (n -= 38)) return r < 0 ? Number.NEGATIVE_INFINITY : Number.POSITIVE_INFINITY;
        var a = r * e.x * .00390625;
        return a += r * e.y * (1 / 65536), (a += r * e.z * (1 / 16777216)) * Math.pow(10, n)
    }, Q.packedLength = 16, Q.pack = function (e, t, n) {
        return n = I.defaultValue(n, 0), t[n++] = e[0], t[n++] = e[1], t[n++] = e[2], t[n++] = e[3], t[n++] = e[4], t[n++] = e[5], t[n++] = e[6], t[n++] = e[7], t[n++] = e[8], t[n++] = e[9], t[n++] = e[10], t[n++] = e[11], t[n++] = e[12], t[n++] = e[13], t[n++] = e[14], t[n] = e[15], t
    }, Q.unpack = function (e, t, n) {
        return t = I.defaultValue(t, 0), I.defined(n) || (n = new Q), n[0] = e[t++], n[1] = e[t++], n[2] = e[t++], n[3] = e[t++], n[4] = e[t++], n[5] = e[t++], n[6] = e[t++], n[7] = e[t++], n[8] = e[t++], n[9] = e[t++], n[10] = e[t++], n[11] = e[t++], n[12] = e[t++], n[13] = e[t++], n[14] = e[t++], n[15] = e[t], n
    }, Q.clone = function (e, t) {
        if (I.defined(e)) return I.defined(t) ? (t[0] = e[0], t[1] = e[1], t[2] = e[2], t[3] = e[3], t[4] = e[4], t[5] = e[5], t[6] = e[6], t[7] = e[7], t[8] = e[8], t[9] = e[9], t[10] = e[10], t[11] = e[11], t[12] = e[12], t[13] = e[13], t[14] = e[14], t[15] = e[15], t) : new Q(e[0], e[4], e[8], e[12], e[1], e[5], e[9], e[13], e[2], e[6], e[10], e[14], e[3], e[7], e[11], e[15])
    }, Q.fromArray = Q.unpack, Q.fromColumnMajorArray = function (e, t) {
        return Q.clone(e, t)
    }, Q.fromRowMajorArray = function (e, t) {
        return I.defined(t) ? (t[0] = e[0], t[1] = e[4], t[2] = e[8], t[3] = e[12], t[4] = e[1], t[5] = e[5], t[6] = e[9], t[7] = e[13], t[8] = e[2], t[9] = e[6], t[10] = e[10], t[11] = e[14], t[12] = e[3], t[13] = e[7], t[14] = e[11], t[15] = e[15], t) : new Q(e[0], e[1], e[2], e[3], e[4], e[5], e[6], e[7], e[8], e[9], e[10], e[11], e[12], e[13], e[14], e[15])
    }, Q.fromRotationTranslation = function (e, t, n) {
        return t = I.defaultValue(t, P.Cartesian3.ZERO), I.defined(n) ? (n[0] = e[0], n[1] = e[1], n[2] = e[2], n[3] = 0, n[4] = e[3], n[5] = e[4], n[6] = e[5], n[7] = 0, n[8] = e[6], n[9] = e[7], n[10] = e[8], n[11] = 0, n[12] = t.x, n[13] = t.y, n[14] = t.z, n[15] = 1, n) : new Q(e[0], e[3], e[6], t.x, e[1], e[4], e[7], t.y, e[2], e[5], e[8], t.z, 0, 0, 0, 1)
    }, Q.fromTranslationQuaternionRotationScale = function (e, t, n, r) {
        I.defined(r) || (r = new Q);
        var a = n.x, i = n.y, s = n.z, o = t.x * t.x, u = t.x * t.y, l = t.x * t.z, c = t.x * t.w, d = t.y * t.y,
            f = t.y * t.z, h = t.y * t.w, p = t.z * t.z, m = t.z * t.w, y = t.w * t.w, v = o - d - p + y,
            C = 2 * (u - m), w = 2 * (l + h), g = 2 * (u + m), x = d - o - p + y, E = 2 * (f - c), O = 2 * (l - h),
            _ = 2 * (f + c), S = -o - d + p + y;
        return r[0] = v * a, r[1] = g * a, r[2] = O * a, r[3] = 0, r[4] = C * i, r[5] = x * i, r[6] = _ * i, r[7] = 0, r[8] = w * s, r[9] = E * s, r[10] = S * s, r[11] = 0, r[12] = e.x, r[13] = e.y, r[14] = e.z, r[15] = 1, r
    }, Q.fromTranslationRotationScale = function (e, t) {
        return Q.fromTranslationQuaternionRotationScale(e.translation, e.rotation, e.scale, t)
    }, Q.fromTranslation = function (e, t) {
        return Q.fromRotationTranslation(J.IDENTITY, e, t)
    }, Q.fromScale = function (e, t) {
        return I.defined(t) ? (t[0] = e.x, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = e.y, t[6] = 0, t[7] = 0, t[8] = 0, t[9] = 0, t[10] = e.z, t[11] = 0, t[12] = 0, t[13] = 0, t[14] = 0, t[15] = 1, t) : new Q(e.x, 0, 0, 0, 0, e.y, 0, 0, 0, 0, e.z, 0, 0, 0, 0, 1)
    }, Q.fromUniformScale = function (e, t) {
        return I.defined(t) ? (t[0] = e, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = e, t[6] = 0, t[7] = 0, t[8] = 0, t[9] = 0, t[10] = e, t[11] = 0, t[12] = 0, t[13] = 0, t[14] = 0, t[15] = 1, t) : new Q(e, 0, 0, 0, 0, e, 0, 0, 0, 0, e, 0, 0, 0, 0, 1)
    };
    var g = new P.Cartesian3, x = new P.Cartesian3, E = new P.Cartesian3;
    Q.fromCamera = function (e, t) {
        var n = e.position, r = e.direction, a = e.up;
        P.Cartesian3.normalize(r, g), P.Cartesian3.normalize(P.Cartesian3.cross(g, a, x), x), P.Cartesian3.normalize(P.Cartesian3.cross(x, g, E), E);
        var i = x.x, s = x.y, o = x.z, u = g.x, l = g.y, c = g.z, d = E.x, f = E.y, h = E.z, p = n.x, m = n.y, y = n.z,
            v = i * -p + s * -m + o * -y, C = d * -p + f * -m + h * -y, w = u * p + l * m + c * y;
        return I.defined(t) ? (t[0] = i, t[1] = d, t[2] = -u, t[3] = 0, t[4] = s, t[5] = f, t[6] = -l, t[7] = 0, t[8] = o, t[9] = h, t[10] = -c, t[11] = 0, t[12] = v, t[13] = C, t[14] = w, t[15] = 1, t) : new Q(i, s, o, v, d, f, h, C, -u, -l, -c, w, 0, 0, 0, 1)
    }, Q.computePerspectiveFieldOfView = function (e, t, n, r, a) {
        var i = 1 / Math.tan(.5 * e), s = i / t, o = (r + n) / (n - r), u = 2 * r * n / (n - r);
        return a[0] = s, a[1] = 0, a[2] = 0, a[3] = 0, a[4] = 0, a[5] = i, a[6] = 0, a[7] = 0, a[8] = 0, a[9] = 0, a[10] = o, a[11] = -1, a[12] = 0, a[13] = 0, a[14] = u, a[15] = 0, a
    }, Q.computeOrthographicOffCenter = function (e, t, n, r, a, i, s) {
        var o = 1 / (t - e), u = 1 / (r - n), l = 1 / (i - a), c = -(t + e) * o, d = -(r + n) * u, f = -(i + a) * l;
        return o *= 2, u *= 2, l *= -2, s[0] = o, s[1] = 0, s[2] = 0, s[3] = 0, s[4] = 0, s[5] = u, s[6] = 0, s[7] = 0, s[8] = 0, s[9] = 0, s[10] = l, s[11] = 0, s[12] = c, s[13] = d, s[14] = f, s[15] = 1, s
    }, Q.computePerspectiveOffCenter = function (e, t, n, r, a, i, s) {
        var o = 2 * a / (t - e), u = 2 * a / (r - n), l = (t + e) / (t - e), c = (r + n) / (r - n),
            d = -(i + a) / (i - a), f = -2 * i * a / (i - a);
        return s[0] = o, s[1] = 0, s[2] = 0, s[3] = 0, s[4] = 0, s[5] = u, s[6] = 0, s[7] = 0, s[8] = l, s[9] = c, s[10] = d, s[11] = -1, s[12] = 0, s[13] = 0, s[14] = f, s[15] = 0, s
    }, Q.computeInfinitePerspectiveOffCenter = function (e, t, n, r, a, i) {
        var s = 2 * a / (t - e), o = 2 * a / (r - n), u = (t + e) / (t - e), l = (r + n) / (r - n), c = -2 * a;
        return i[0] = s, i[1] = 0, i[2] = 0, i[3] = 0, i[4] = 0, i[5] = o, i[6] = 0, i[7] = 0, i[8] = u, i[9] = l, i[10] = -1, i[11] = -1, i[12] = 0, i[13] = 0, i[14] = c, i[15] = 0, i
    }, Q.computeViewportTransformation = function (e, t, n, r) {
        e = I.defaultValue(e, I.defaultValue.EMPTY_OBJECT);
        var a = I.defaultValue(e.x, 0), i = I.defaultValue(e.y, 0), s = I.defaultValue(e.width, 0),
            o = I.defaultValue(e.height, 0);
        t = I.defaultValue(t, 0);
        var u = .5 * s, l = .5 * o, c = .5 * ((n = I.defaultValue(n, 1)) - t), d = u, f = l, h = c, p = a + u,
            m = i + l, y = t + c;
        return r[0] = d, r[1] = 0, r[2] = 0, r[3] = 0, r[4] = 0, r[5] = f, r[6] = 0, r[7] = 0, r[8] = 0, r[9] = 0, r[10] = h, r[11] = 0, r[12] = p, r[13] = m, r[14] = y, r[15] = 1, r
    }, Q.computeView = function (e, t, n, r, a) {
        return a[0] = r.x, a[1] = n.x, a[2] = -t.x, a[3] = 0, a[4] = r.y, a[5] = n.y, a[6] = -t.y, a[7] = 0, a[8] = r.z, a[9] = n.z, a[10] = -t.z, a[11] = 0, a[12] = -P.Cartesian3.dot(r, e), a[13] = -P.Cartesian3.dot(n, e), a[14] = P.Cartesian3.dot(t, e), a[15] = 1, a
    }, Q.toArray = function (e, t) {
        return I.defined(t) ? (t[0] = e[0], t[1] = e[1], t[2] = e[2], t[3] = e[3], t[4] = e[4], t[5] = e[5], t[6] = e[6], t[7] = e[7], t[8] = e[8], t[9] = e[9], t[10] = e[10], t[11] = e[11], t[12] = e[12], t[13] = e[13], t[14] = e[14], t[15] = e[15], t) : [e[0], e[1], e[2], e[3], e[4], e[5], e[6], e[7], e[8], e[9], e[10], e[11], e[12], e[13], e[14], e[15]]
    }, Q.getElementIndex = function (e, t) {
        return 4 * e + t
    }, Q.getColumn = function (e, t, n) {
        var r = 4 * t, a = e[r], i = e[1 + r], s = e[2 + r], o = e[3 + r];
        return n.x = a, n.y = i, n.z = s, n.w = o, n
    }, Q.setColumn = function (e, t, n, r) {
        var a = 4 * t;
        return (r = Q.clone(e, r))[a] = n.x, r[1 + a] = n.y, r[2 + a] = n.z, r[3 + a] = n.w, r
    }, Q.setTranslation = function (e, t, n) {
        return n[0] = e[0], n[1] = e[1], n[2] = e[2], n[3] = e[3], n[4] = e[4], n[5] = e[5], n[6] = e[6], n[7] = e[7], n[8] = e[8], n[9] = e[9], n[10] = e[10], n[11] = e[11], n[12] = t.x, n[13] = t.y, n[14] = t.z, n[15] = e[15], n
    };
    var w = new P.Cartesian3;
    Q.setScale = function (e, t, n) {
        var r = Q.getScale(e, w), a = P.Cartesian3.divideComponents(t, r, w);
        return Q.multiplyByScale(e, a, n)
    }, Q.getRow = function (e, t, n) {
        var r = e[t], a = e[t + 4], i = e[t + 8], s = e[t + 12];
        return n.x = r, n.y = a, n.z = i, n.w = s, n
    }, Q.setRow = function (e, t, n, r) {
        return (r = Q.clone(e, r))[t] = n.x, r[t + 4] = n.y, r[t + 8] = n.z, r[t + 12] = n.w, r
    };
    var O = new P.Cartesian3;
    Q.getScale = function (e, t) {
        return t.x = P.Cartesian3.magnitude(P.Cartesian3.fromElements(e[0], e[1], e[2], O)), t.y = P.Cartesian3.magnitude(P.Cartesian3.fromElements(e[4], e[5], e[6], O)), t.z = P.Cartesian3.magnitude(P.Cartesian3.fromElements(e[8], e[9], e[10], O)), t
    };
    var _ = new P.Cartesian3;
    Q.getMaximumScale = function (e) {
        return Q.getScale(e, _), P.Cartesian3.maximumComponent(_)
    }, Q.multiply = function (e, t, n) {
        var r = e[0], a = e[1], i = e[2], s = e[3], o = e[4], u = e[5], l = e[6], c = e[7], d = e[8], f = e[9],
            h = e[10], p = e[11], m = e[12], y = e[13], v = e[14], C = e[15], w = t[0], g = t[1], x = t[2], E = t[3],
            O = t[4], _ = t[5], S = t[6], b = t[7], M = t[8], R = t[9], A = t[10], T = t[11], q = t[12], z = t[13],
            I = t[14], P = t[15], D = r * w + o * g + d * x + m * E, N = a * w + u * g + f * x + y * E,
            U = i * w + l * g + h * x + v * E, F = s * w + c * g + p * x + C * E, V = r * O + o * _ + d * S + m * b,
            L = a * O + u * _ + f * S + y * b, B = i * O + l * _ + h * S + v * b, W = s * O + c * _ + p * S + C * b,
            j = r * M + o * R + d * A + m * T, k = a * M + u * R + f * A + y * T, Y = i * M + l * R + h * A + v * T,
            Z = s * M + c * R + p * A + C * T, X = r * q + o * z + d * I + m * P, H = a * q + u * z + f * I + y * P,
            J = i * q + l * z + h * I + v * P, G = s * q + c * z + p * I + C * P;
        return n[0] = D, n[1] = N, n[2] = U, n[3] = F, n[4] = V, n[5] = L, n[6] = B, n[7] = W, n[8] = j, n[9] = k, n[10] = Y, n[11] = Z, n[12] = X, n[13] = H, n[14] = J, n[15] = G, n
    }, Q.add = function (e, t, n) {
        return n[0] = e[0] + t[0], n[1] = e[1] + t[1], n[2] = e[2] + t[2], n[3] = e[3] + t[3], n[4] = e[4] + t[4], n[5] = e[5] + t[5], n[6] = e[6] + t[6], n[7] = e[7] + t[7], n[8] = e[8] + t[8], n[9] = e[9] + t[9], n[10] = e[10] + t[10], n[11] = e[11] + t[11], n[12] = e[12] + t[12], n[13] = e[13] + t[13], n[14] = e[14] + t[14], n[15] = e[15] + t[15], n
    }, Q.subtract = function (e, t, n) {
        return n[0] = e[0] - t[0], n[1] = e[1] - t[1], n[2] = e[2] - t[2], n[3] = e[3] - t[3], n[4] = e[4] - t[4], n[5] = e[5] - t[5], n[6] = e[6] - t[6], n[7] = e[7] - t[7], n[8] = e[8] - t[8], n[9] = e[9] - t[9], n[10] = e[10] - t[10], n[11] = e[11] - t[11], n[12] = e[12] - t[12], n[13] = e[13] - t[13], n[14] = e[14] - t[14], n[15] = e[15] - t[15], n
    }, Q.multiplyTransformation = function (e, t, n) {
        var r = e[0], a = e[1], i = e[2], s = e[4], o = e[5], u = e[6], l = e[8], c = e[9], d = e[10], f = e[12],
            h = e[13], p = e[14], m = t[0], y = t[1], v = t[2], C = t[4], w = t[5], g = t[6], x = t[8], E = t[9],
            O = t[10], _ = t[12], S = t[13], b = t[14], M = r * m + s * y + l * v, R = a * m + o * y + c * v,
            A = i * m + u * y + d * v, T = r * C + s * w + l * g, q = a * C + o * w + c * g, z = i * C + u * w + d * g,
            I = r * x + s * E + l * O, P = a * x + o * E + c * O, D = i * x + u * E + d * O,
            N = r * _ + s * S + l * b + f, U = a * _ + o * S + c * b + h, F = i * _ + u * S + d * b + p;
        return n[0] = M, n[1] = R, n[2] = A, n[3] = 0, n[4] = T, n[5] = q, n[6] = z, n[7] = 0, n[8] = I, n[9] = P, n[10] = D, n[11] = 0, n[12] = N, n[13] = U, n[14] = F, n[15] = 1, n
    }, Q.multiplyByMatrix3 = function (e, t, n) {
        var r = e[0], a = e[1], i = e[2], s = e[4], o = e[5], u = e[6], l = e[8], c = e[9], d = e[10], f = t[0],
            h = t[1], p = t[2], m = t[3], y = t[4], v = t[5], C = t[6], w = t[7], g = t[8], x = r * f + s * h + l * p,
            E = a * f + o * h + c * p, O = i * f + u * h + d * p, _ = r * m + s * y + l * v, S = a * m + o * y + c * v,
            b = i * m + u * y + d * v, M = r * C + s * w + l * g, R = a * C + o * w + c * g, A = i * C + u * w + d * g;
        return n[0] = x, n[1] = E, n[2] = O, n[3] = 0, n[4] = _, n[5] = S, n[6] = b, n[7] = 0, n[8] = M, n[9] = R, n[10] = A, n[11] = 0, n[12] = e[12], n[13] = e[13], n[14] = e[14], n[15] = e[15], n
    }, Q.multiplyByTranslation = function (e, t, n) {
        var r = t.x, a = t.y, i = t.z, s = r * e[0] + a * e[4] + i * e[8] + e[12],
            o = r * e[1] + a * e[5] + i * e[9] + e[13], u = r * e[2] + a * e[6] + i * e[10] + e[14];
        return n[0] = e[0], n[1] = e[1], n[2] = e[2], n[3] = e[3], n[4] = e[4], n[5] = e[5], n[6] = e[6], n[7] = e[7], n[8] = e[8], n[9] = e[9], n[10] = e[10], n[11] = e[11], n[12] = s, n[13] = o, n[14] = u, n[15] = e[15], n
    };
    var S = new P.Cartesian3;
    Q.multiplyByUniformScale = function (e, t, n) {
        return S.x = t, S.y = t, S.z = t, Q.multiplyByScale(e, S, n)
    }, Q.multiplyByScale = function (e, t, n) {
        var r = t.x, a = t.y, i = t.z;
        return 1 === r && 1 === a && 1 === i ? Q.clone(e, n) : (n[0] = r * e[0], n[1] = r * e[1], n[2] = r * e[2], n[3] = 0, n[4] = a * e[4], n[5] = a * e[5], n[6] = a * e[6], n[7] = 0, n[8] = i * e[8], n[9] = i * e[9], n[10] = i * e[10], n[11] = 0, n[12] = e[12], n[13] = e[13], n[14] = e[14], n[15] = 1, n)
    }, Q.multiplyByVector = function (e, t, n) {
        var r = t.x, a = t.y, i = t.z, s = t.w, o = e[0] * r + e[4] * a + e[8] * i + e[12] * s,
            u = e[1] * r + e[5] * a + e[9] * i + e[13] * s, l = e[2] * r + e[6] * a + e[10] * i + e[14] * s,
            c = e[3] * r + e[7] * a + e[11] * i + e[15] * s;
        return n.x = o, n.y = u, n.z = l, n.w = c, n
    }, Q.multiplyByPointAsVector = function (e, t, n) {
        var r = t.x, a = t.y, i = t.z, s = e[0] * r + e[4] * a + e[8] * i, o = e[1] * r + e[5] * a + e[9] * i,
            u = e[2] * r + e[6] * a + e[10] * i;
        return n.x = s, n.y = o, n.z = u, n
    }, Q.multiplyByPoint = function (e, t, n) {
        var r = t.x, a = t.y, i = t.z, s = e[0] * r + e[4] * a + e[8] * i + e[12],
            o = e[1] * r + e[5] * a + e[9] * i + e[13], u = e[2] * r + e[6] * a + e[10] * i + e[14];
        return n.x = s, n.y = o, n.z = u, n
    }, Q.multiplyByScalar = function (e, t, n) {
        return n[0] = e[0] * t, n[1] = e[1] * t, n[2] = e[2] * t, n[3] = e[3] * t, n[4] = e[4] * t, n[5] = e[5] * t, n[6] = e[6] * t, n[7] = e[7] * t, n[8] = e[8] * t, n[9] = e[9] * t, n[10] = e[10] * t, n[11] = e[11] * t, n[12] = e[12] * t, n[13] = e[13] * t, n[14] = e[14] * t, n[15] = e[15] * t, n
    }, Q.negate = function (e, t) {
        return t[0] = -e[0], t[1] = -e[1], t[2] = -e[2], t[3] = -e[3], t[4] = -e[4], t[5] = -e[5], t[6] = -e[6], t[7] = -e[7], t[8] = -e[8], t[9] = -e[9], t[10] = -e[10], t[11] = -e[11], t[12] = -e[12], t[13] = -e[13], t[14] = -e[14], t[15] = -e[15], t
    }, Q.transpose = function (e, t) {
        var n = e[1], r = e[2], a = e[3], i = e[6], s = e[7], o = e[11];
        return t[0] = e[0], t[1] = e[4], t[2] = e[8], t[3] = e[12], t[4] = n, t[5] = e[5], t[6] = e[9], t[7] = e[13], t[8] = r, t[9] = i, t[10] = e[10], t[11] = e[14], t[12] = a, t[13] = s, t[14] = o, t[15] = e[15], t
    }, Q.abs = function (e, t) {
        return t[0] = Math.abs(e[0]), t[1] = Math.abs(e[1]), t[2] = Math.abs(e[2]), t[3] = Math.abs(e[3]), t[4] = Math.abs(e[4]), t[5] = Math.abs(e[5]), t[6] = Math.abs(e[6]), t[7] = Math.abs(e[7]), t[8] = Math.abs(e[8]), t[9] = Math.abs(e[9]), t[10] = Math.abs(e[10]), t[11] = Math.abs(e[11]), t[12] = Math.abs(e[12]), t[13] = Math.abs(e[13]), t[14] = Math.abs(e[14]), t[15] = Math.abs(e[15]), t
    }, Q.equals = function (e, t) {
        return e === t || I.defined(e) && I.defined(t) && e[12] === t[12] && e[13] === t[13] && e[14] === t[14] && e[0] === t[0] && e[1] === t[1] && e[2] === t[2] && e[4] === t[4] && e[5] === t[5] && e[6] === t[6] && e[8] === t[8] && e[9] === t[9] && e[10] === t[10] && e[3] === t[3] && e[7] === t[7] && e[11] === t[11] && e[15] === t[15]
    }, Q.equalsEpsilon = function (e, t, n) {
        return e === t || I.defined(e) && I.defined(t) && Math.abs(e[0] - t[0]) <= n && Math.abs(e[1] - t[1]) <= n && Math.abs(e[2] - t[2]) <= n && Math.abs(e[3] - t[3]) <= n && Math.abs(e[4] - t[4]) <= n && Math.abs(e[5] - t[5]) <= n && Math.abs(e[6] - t[6]) <= n && Math.abs(e[7] - t[7]) <= n && Math.abs(e[8] - t[8]) <= n && Math.abs(e[9] - t[9]) <= n && Math.abs(e[10] - t[10]) <= n && Math.abs(e[11] - t[11]) <= n && Math.abs(e[12] - t[12]) <= n && Math.abs(e[13] - t[13]) <= n && Math.abs(e[14] - t[14]) <= n && Math.abs(e[15] - t[15]) <= n
    }, Q.getTranslation = function (e, t) {
        return t.x = e[12], t.y = e[13], t.z = e[14], t
    }, Q.getMatrix3 = function (e, t) {
        return t[0] = e[0], t[1] = e[1], t[2] = e[2], t[3] = e[4], t[4] = e[5], t[5] = e[6], t[6] = e[8], t[7] = e[9], t[8] = e[10], t
    };
    var K = new J, $ = new J, ee = new G, te = new G(0, 0, 0, 1);

    function D(e, t) {
        this.center = P.Cartesian3.clone(I.defaultValue(e, P.Cartesian3.ZERO)), this.radius = I.defaultValue(t, 0)
    }

    Q.inverse = function (e, t) {
        var n = e[0], r = e[4], a = e[8], i = e[12], s = e[1], o = e[5], u = e[9], l = e[13], c = e[2], d = e[6],
            f = e[10], h = e[14], p = e[3], m = e[7], y = e[11], v = e[15], C = f * v, w = h * y, g = d * v, x = h * m,
            E = d * y, O = f * m, _ = c * v, S = h * p, b = c * y, M = f * p, R = c * m, A = d * p,
            T = C * o + x * u + E * l - (w * o + g * u + O * l), q = w * s + _ * u + M * l - (C * s + S * u + b * l),
            z = g * s + S * o + R * l - (x * s + _ * o + A * l), I = O * s + b * o + A * u - (E * s + M * o + R * u),
            P = w * r + g * a + O * i - (C * r + x * a + E * i), D = C * n + S * a + b * i - (w * n + _ * a + M * i),
            N = x * n + _ * r + A * i - (g * n + S * r + R * i), U = E * n + M * r + R * a - (O * n + b * r + A * a),
            F = (C = a * l) * m + (x = i * o) * y + (E = r * u) * v - ((w = i * u) * m + (g = r * l) * y + (O = a * o) * v),
            V = w * p + (_ = n * l) * y + (M = a * s) * v - (C * p + (S = i * s) * y + (b = n * u) * v),
            L = g * p + S * m + (R = n * o) * v - (x * p + _ * m + (A = r * s) * v),
            B = O * p + b * m + A * y - (E * p + M * m + R * y), W = g * f + O * h + w * d - (E * h + C * d + x * f),
            j = b * h + C * c + S * f - (_ * f + M * h + w * c), k = _ * d + A * h + x * c - (R * h + g * c + S * d),
            Y = R * f + E * c + M * d - (b * d + A * f + O * c), Z = n * T + r * q + a * z + i * I;
        if (Math.abs(Z) < X.CesiumMath.EPSILON21) {
            if (J.equalsEpsilon(Q.getMatrix3(e, K), $, X.CesiumMath.EPSILON7) && G.equals(Q.getRow(e, 3, ee), te)) return t[0] = 0, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = 0, t[6] = 0, t[7] = 0, t[8] = 0, t[9] = 0, t[10] = 0, t[11] = 0, t[12] = -e[12], t[13] = -e[13], t[14] = -e[14], t[15] = 1, t;
            throw new H.RuntimeError("matrix is not invertible because its determinate is zero.")
        }
        return Z = 1 / Z, t[0] = T * Z, t[1] = q * Z, t[2] = z * Z, t[3] = I * Z, t[4] = P * Z, t[5] = D * Z, t[6] = N * Z, t[7] = U * Z, t[8] = F * Z, t[9] = V * Z, t[10] = L * Z, t[11] = B * Z, t[12] = W * Z, t[13] = j * Z, t[14] = k * Z, t[15] = Y * Z, t
    }, Q.inverseTransformation = function (e, t) {
        var n = e[0], r = e[1], a = e[2], i = e[4], s = e[5], o = e[6], u = e[8], l = e[9], c = e[10], d = e[12],
            f = e[13], h = e[14], p = -n * d - r * f - a * h, m = -i * d - s * f - o * h, y = -u * d - l * f - c * h;
        return t[0] = n, t[1] = i, t[2] = u, t[3] = 0, t[4] = r, t[5] = s, t[6] = l, t[7] = 0, t[8] = a, t[9] = o, t[10] = c, t[11] = 0, t[12] = p, t[13] = m, t[14] = y, t[15] = 1, t
    }, Q.IDENTITY = Object.freeze(new Q(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1)), Q.ZERO = Object.freeze(new Q(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)), Q.COLUMN0ROW0 = 0, Q.COLUMN0ROW1 = 1, Q.COLUMN0ROW2 = 2, Q.COLUMN0ROW3 = 3, Q.COLUMN1ROW0 = 4, Q.COLUMN1ROW1 = 5, Q.COLUMN1ROW2 = 6, Q.COLUMN1ROW3 = 7, Q.COLUMN2ROW0 = 8, Q.COLUMN2ROW1 = 9, Q.COLUMN2ROW2 = 10, Q.COLUMN2ROW3 = 11, Q.COLUMN3ROW0 = 12, Q.COLUMN3ROW1 = 13, Q.COLUMN3ROW2 = 14, Q.COLUMN3ROW3 = 15, Object.defineProperties(Q.prototype, {
        length: {
            get: function () {
                return Q.packedLength
            }
        }
    }), Q.prototype.clone = function (e) {
        return Q.clone(this, e)
    }, Q.prototype.equals = function (e) {
        return Q.equals(this, e)
    }, Q.equalsArray = function (e, t, n) {
        return e[0] === t[n] && e[1] === t[n + 1] && e[2] === t[n + 2] && e[3] === t[n + 3] && e[4] === t[n + 4] && e[5] === t[n + 5] && e[6] === t[n + 6] && e[7] === t[n + 7] && e[8] === t[n + 8] && e[9] === t[n + 9] && e[10] === t[n + 10] && e[11] === t[n + 11] && e[12] === t[n + 12] && e[13] === t[n + 13] && e[14] === t[n + 14] && e[15] === t[n + 15]
    }, Q.prototype.equalsEpsilon = function (e, t) {
        return Q.equalsEpsilon(this, e, t)
    }, Q.prototype.toString = function () {
        return "(" + this[0] + ", " + this[4] + ", " + this[8] + ", " + this[12] + ")\n(" + this[1] + ", " + this[5] + ", " + this[9] + ", " + this[13] + ")\n(" + this[2] + ", " + this[6] + ", " + this[10] + ", " + this[14] + ")\n(" + this[3] + ", " + this[7] + ", " + this[11] + ", " + this[15] + ")"
    };
    var N = new P.Cartesian3, U = new P.Cartesian3, F = new P.Cartesian3, V = new P.Cartesian3, L = new P.Cartesian3,
        B = new P.Cartesian3, W = new P.Cartesian3, j = new P.Cartesian3, k = new P.Cartesian3, Y = new P.Cartesian3,
        Z = new P.Cartesian3, ne = new P.Cartesian3, b = 4 / 3 * X.CesiumMath.PI;
    D.fromPoints = function (e, t) {
        if (I.defined(t) || (t = new D), !I.defined(e) || 0 === e.length) return t.center = P.Cartesian3.clone(P.Cartesian3.ZERO, t.center), t.radius = 0, t;
        var n, r = P.Cartesian3.clone(e[0], W), a = P.Cartesian3.clone(r, N), i = P.Cartesian3.clone(r, U),
            s = P.Cartesian3.clone(r, F), o = P.Cartesian3.clone(r, V), u = P.Cartesian3.clone(r, L),
            l = P.Cartesian3.clone(r, B), c = e.length;
        for (n = 1; n < c; n++) {
            P.Cartesian3.clone(e[n], r);
            var d = r.x, f = r.y, h = r.z;
            d < a.x && P.Cartesian3.clone(r, a), d > o.x && P.Cartesian3.clone(r, o), f < i.y && P.Cartesian3.clone(r, i), f > u.y && P.Cartesian3.clone(r, u), h < s.z && P.Cartesian3.clone(r, s), h > l.z && P.Cartesian3.clone(r, l)
        }
        var p = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(o, a, j)),
            m = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(u, i, j)),
            y = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(l, s, j)), v = a, C = o, w = p;
        w < m && (w = m, v = i, C = u), w < y && (w = y, v = s, C = l);
        var g = k;
        g.x = .5 * (v.x + C.x), g.y = .5 * (v.y + C.y), g.z = .5 * (v.z + C.z);
        var x = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(C, g, j)), E = Math.sqrt(x), O = Y;
        O.x = a.x, O.y = i.y, O.z = s.z;
        var _ = Z;
        _.x = o.x, _.y = u.y, _.z = l.z;
        var S = P.Cartesian3.midpoint(O, _, ne), b = 0;
        for (n = 0; n < c; n++) {
            P.Cartesian3.clone(e[n], r);
            var M = P.Cartesian3.magnitude(P.Cartesian3.subtract(r, S, j));
            b < M && (b = M);
            var R = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(r, g, j));
            if (x < R) {
                var A = Math.sqrt(R);
                x = (E = .5 * (E + A)) * E;
                var T = A - E;
                g.x = (E * g.x + T * r.x) / A, g.y = (E * g.y + T * r.y) / A, g.z = (E * g.z + T * r.z) / A
            }
        }
        return E < b ? (P.Cartesian3.clone(g, t.center), t.radius = E) : (P.Cartesian3.clone(S, t.center), t.radius = b), t
    };
    var M = new t, R = new P.Cartesian3, A = new P.Cartesian3, T = new P.Cartographic, q = new P.Cartographic;
    D.fromRectangle2D = function (e, t, n) {
        return D.fromRectangleWithHeights2D(e, t, 0, 0, n)
    }, D.fromRectangleWithHeights2D = function (e, t, n, r, a) {
        if (I.defined(a) || (a = new D), !I.defined(e)) return a.center = P.Cartesian3.clone(P.Cartesian3.ZERO, a.center), a.radius = 0, a;
        t = I.defaultValue(t, M), P.Rectangle.southwest(e, T), T.height = n, P.Rectangle.northeast(e, q), q.height = r;
        var i = t.project(T, R), s = t.project(q, A), o = s.x - i.x, u = s.y - i.y, l = s.z - i.z;
        a.radius = .5 * Math.sqrt(o * o + u * u + l * l);
        var c = a.center;
        return c.x = i.x + .5 * o, c.y = i.y + .5 * u, c.z = i.z + .5 * l, a
    };
    var z = [];
    D.fromRectangle3D = function (e, t, n, r) {
        if (t = I.defaultValue(t, P.Ellipsoid.WGS84), n = I.defaultValue(n, 0), I.defined(r) || (r = new D), !I.defined(e)) return r.center = P.Cartesian3.clone(P.Cartesian3.ZERO, r.center), r.radius = 0, r;
        var a = P.Rectangle.subsample(e, t, n, z);
        return D.fromPoints(a, r)
    }, D.fromVertices = function (e, t, n, r) {
        if (I.defined(r) || (r = new D), !I.defined(e) || 0 === e.length) return r.center = P.Cartesian3.clone(P.Cartesian3.ZERO, r.center), r.radius = 0, r;
        t = I.defaultValue(t, P.Cartesian3.ZERO), n = I.defaultValue(n, 3);
        var a = W;
        a.x = e[0] + t.x, a.y = e[1] + t.y, a.z = e[2] + t.z;
        var i, s = P.Cartesian3.clone(a, N), o = P.Cartesian3.clone(a, U), u = P.Cartesian3.clone(a, F),
            l = P.Cartesian3.clone(a, V), c = P.Cartesian3.clone(a, L), d = P.Cartesian3.clone(a, B), f = e.length;
        for (i = 0; i < f; i += n) {
            var h = e[i] + t.x, p = e[i + 1] + t.y, m = e[i + 2] + t.z;
            a.x = h, a.y = p, a.z = m, h < s.x && P.Cartesian3.clone(a, s), h > l.x && P.Cartesian3.clone(a, l), p < o.y && P.Cartesian3.clone(a, o), p > c.y && P.Cartesian3.clone(a, c), m < u.z && P.Cartesian3.clone(a, u), m > d.z && P.Cartesian3.clone(a, d)
        }
        var y = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(l, s, j)),
            v = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(c, o, j)),
            C = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(d, u, j)), w = s, g = l, x = y;
        x < v && (x = v, w = o, g = c), x < C && (x = C, w = u, g = d);
        var E = k;
        E.x = .5 * (w.x + g.x), E.y = .5 * (w.y + g.y), E.z = .5 * (w.z + g.z);
        var O = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(g, E, j)), _ = Math.sqrt(O), S = Y;
        S.x = s.x, S.y = o.y, S.z = u.z;
        var b = Z;
        b.x = l.x, b.y = c.y, b.z = d.z;
        var M = P.Cartesian3.midpoint(S, b, ne), R = 0;
        for (i = 0; i < f; i += n) {
            a.x = e[i] + t.x, a.y = e[i + 1] + t.y, a.z = e[i + 2] + t.z;
            var A = P.Cartesian3.magnitude(P.Cartesian3.subtract(a, M, j));
            R < A && (R = A);
            var T = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(a, E, j));
            if (O < T) {
                var q = Math.sqrt(T);
                O = (_ = .5 * (_ + q)) * _;
                var z = q - _;
                E.x = (_ * E.x + z * a.x) / q, E.y = (_ * E.y + z * a.y) / q, E.z = (_ * E.z + z * a.z) / q
            }
        }
        return _ < R ? (P.Cartesian3.clone(E, r.center), r.radius = _) : (P.Cartesian3.clone(M, r.center), r.radius = R), r
    }, D.fromEncodedCartesianVertices = function (e, t, n) {
        if (I.defined(n) || (n = new D), !I.defined(e) || !I.defined(t) || e.length !== t.length || 0 === e.length) return n.center = P.Cartesian3.clone(P.Cartesian3.ZERO, n.center), n.radius = 0, n;
        var r = W;
        r.x = e[0] + t[0], r.y = e[1] + t[1], r.z = e[2] + t[2];
        var a, i = P.Cartesian3.clone(r, N), s = P.Cartesian3.clone(r, U), o = P.Cartesian3.clone(r, F),
            u = P.Cartesian3.clone(r, V), l = P.Cartesian3.clone(r, L), c = P.Cartesian3.clone(r, B), d = e.length;
        for (a = 0; a < d; a += 3) {
            var f = e[a] + t[a], h = e[a + 1] + t[a + 1], p = e[a + 2] + t[a + 2];
            r.x = f, r.y = h, r.z = p, f < i.x && P.Cartesian3.clone(r, i), f > u.x && P.Cartesian3.clone(r, u), h < s.y && P.Cartesian3.clone(r, s), h > l.y && P.Cartesian3.clone(r, l), p < o.z && P.Cartesian3.clone(r, o), p > c.z && P.Cartesian3.clone(r, c)
        }
        var m = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(u, i, j)),
            y = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(l, s, j)),
            v = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(c, o, j)), C = i, w = u, g = m;
        g < y && (g = y, C = s, w = l), g < v && (g = v, C = o, w = c);
        var x = k;
        x.x = .5 * (C.x + w.x), x.y = .5 * (C.y + w.y), x.z = .5 * (C.z + w.z);
        var E = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(w, x, j)), O = Math.sqrt(E), _ = Y;
        _.x = i.x, _.y = s.y, _.z = o.z;
        var S = Z;
        S.x = u.x, S.y = l.y, S.z = c.z;
        var b = P.Cartesian3.midpoint(_, S, ne), M = 0;
        for (a = 0; a < d; a += 3) {
            r.x = e[a] + t[a], r.y = e[a + 1] + t[a + 1], r.z = e[a + 2] + t[a + 2];
            var R = P.Cartesian3.magnitude(P.Cartesian3.subtract(r, b, j));
            M < R && (M = R);
            var A = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(r, x, j));
            if (E < A) {
                var T = Math.sqrt(A);
                E = (O = .5 * (O + T)) * O;
                var q = T - O;
                x.x = (O * x.x + q * r.x) / T, x.y = (O * x.y + q * r.y) / T, x.z = (O * x.z + q * r.z) / T
            }
        }
        return O < M ? (P.Cartesian3.clone(x, n.center), n.radius = O) : (P.Cartesian3.clone(b, n.center), n.radius = M), n
    }, D.fromCornerPoints = function (e, t, n) {
        I.defined(n) || (n = new D);
        var r = P.Cartesian3.midpoint(e, t, n.center);
        return n.radius = P.Cartesian3.distance(r, t), n
    }, D.fromEllipsoid = function (e, t) {
        return I.defined(t) || (t = new D), P.Cartesian3.clone(P.Cartesian3.ZERO, t.center), t.radius = e.maximumRadius, t
    };
    var re = new P.Cartesian3;
    D.fromBoundingSpheres = function (e, t) {
        if (I.defined(t) || (t = new D), !I.defined(e) || 0 === e.length) return t.center = P.Cartesian3.clone(P.Cartesian3.ZERO, t.center), t.radius = 0, t;
        var n = e.length;
        if (1 === n) return D.clone(e[0], t);
        if (2 === n) return D.union(e[0], e[1], t);
        var r, a = [];
        for (r = 0; r < n; r++) a.push(e[r].center);
        var i = (t = D.fromPoints(a, t)).center, s = t.radius;
        for (r = 0; r < n; r++) {
            var o = e[r];
            s = Math.max(s, P.Cartesian3.distance(i, o.center, re) + o.radius)
        }
        return t.radius = s, t
    };
    var ae = new P.Cartesian3, ie = new P.Cartesian3, se = new P.Cartesian3;
    D.fromOrientedBoundingBox = function (e, t) {
        I.defined(t) || (t = new D);
        var n = e.halfAxes, r = J.getColumn(n, 0, ae), a = J.getColumn(n, 1, ie), i = J.getColumn(n, 2, se);
        return P.Cartesian3.add(r, a, r), P.Cartesian3.add(r, i, r), t.center = P.Cartesian3.clone(e.center, t.center), t.radius = P.Cartesian3.magnitude(r), t
    }, D.clone = function (e, t) {
        if (I.defined(e)) return I.defined(t) ? (t.center = P.Cartesian3.clone(e.center, t.center), t.radius = e.radius, t) : new D(e.center, e.radius)
    }, D.packedLength = 4, D.pack = function (e, t, n) {
        n = I.defaultValue(n, 0);
        var r = e.center;
        return t[n++] = r.x, t[n++] = r.y, t[n++] = r.z, t[n] = e.radius, t
    }, D.unpack = function (e, t, n) {
        t = I.defaultValue(t, 0), I.defined(n) || (n = new D);
        var r = n.center;
        return r.x = e[t++], r.y = e[t++], r.z = e[t++], n.radius = e[t], n
    };
    var oe = new P.Cartesian3, ue = new P.Cartesian3;
    D.union = function (e, t, n) {
        I.defined(n) || (n = new D);
        var r = e.center, a = e.radius, i = t.center, s = t.radius, o = P.Cartesian3.subtract(i, r, oe),
            u = P.Cartesian3.magnitude(o);
        if (u + s <= a) return e.clone(n), n;
        if (u + a <= s) return t.clone(n), n;
        var l = .5 * (a + u + s), c = P.Cartesian3.multiplyByScalar(o, (l - a) / u, ue);
        return P.Cartesian3.add(c, r, c), P.Cartesian3.clone(c, n.center), n.radius = l, n
    };
    var le = new P.Cartesian3;
    D.expand = function (e, t, n) {
        n = D.clone(e, n);
        var r = P.Cartesian3.magnitude(P.Cartesian3.subtract(t, n.center, le));
        return r > n.radius && (n.radius = r), n
    }, D.intersectPlane = function (e, t) {
        var n = e.center, r = e.radius, a = t.normal, i = P.Cartesian3.dot(a, n) + t.distance;
        return i < -r ? s.OUTSIDE : i < r ? s.INTERSECTING : s.INSIDE
    }, D.transform = function (e, t, n) {
        return I.defined(n) || (n = new D), n.center = Q.multiplyByPoint(t, e.center, n.center), n.radius = Q.getMaximumScale(t) * e.radius, n
    };
    var ce = new P.Cartesian3;
    D.distanceSquaredTo = function (e, t) {
        var n = P.Cartesian3.subtract(e.center, t, ce);
        return P.Cartesian3.magnitudeSquared(n) - e.radius * e.radius
    }, D.transformWithoutScale = function (e, t, n) {
        return I.defined(n) || (n = new D), n.center = Q.multiplyByPoint(t, e.center, n.center), n.radius = e.radius, n
    };
    var de = new P.Cartesian3;
    D.computePlaneDistances = function (e, t, n, r) {
        I.defined(r) || (r = new o);
        var a = P.Cartesian3.subtract(e.center, t, de), i = P.Cartesian3.dot(n, a);
        return r.start = i - e.radius, r.stop = i + e.radius, r
    };
    for (var fe = new P.Cartesian3, he = new P.Cartesian3, pe = new P.Cartesian3, me = new P.Cartesian3, ye = new P.Cartesian3, ve = new P.Cartographic, Ce = new Array(8), we = 0; we < 8; ++we) Ce[we] = new P.Cartesian3;
    var ge, xe = new t;
    D.projectTo2D = function (e, t, n) {
        var r, a = (t = I.defaultValue(t, xe)).ellipsoid, i = e.center, s = e.radius;
        r = P.Cartesian3.equals(i, P.Cartesian3.ZERO) ? P.Cartesian3.clone(P.Cartesian3.UNIT_X, fe) : a.geodeticSurfaceNormal(i, fe);
        var o = P.Cartesian3.cross(P.Cartesian3.UNIT_Z, r, he);
        P.Cartesian3.normalize(o, o);
        var u = P.Cartesian3.cross(r, o, pe);
        P.Cartesian3.normalize(u, u), P.Cartesian3.multiplyByScalar(r, s, r), P.Cartesian3.multiplyByScalar(u, s, u), P.Cartesian3.multiplyByScalar(o, s, o);
        var l = P.Cartesian3.negate(u, ye), c = P.Cartesian3.negate(o, me), d = Ce, f = d[0];
        P.Cartesian3.add(r, u, f), P.Cartesian3.add(f, o, f), f = d[1], P.Cartesian3.add(r, u, f), P.Cartesian3.add(f, c, f), f = d[2], P.Cartesian3.add(r, l, f), P.Cartesian3.add(f, c, f), f = d[3], P.Cartesian3.add(r, l, f), P.Cartesian3.add(f, o, f), P.Cartesian3.negate(r, r), f = d[4], P.Cartesian3.add(r, u, f), P.Cartesian3.add(f, o, f), f = d[5], P.Cartesian3.add(r, u, f), P.Cartesian3.add(f, c, f), f = d[6], P.Cartesian3.add(r, l, f), P.Cartesian3.add(f, c, f), f = d[7], P.Cartesian3.add(r, l, f), P.Cartesian3.add(f, o, f);
        for (var h = d.length, p = 0; p < h; ++p) {
            var m = d[p];
            P.Cartesian3.add(i, m, m);
            var y = a.cartesianToCartographic(m, ve);
            t.project(y, m)
        }
        var v = (i = (n = D.fromPoints(d, n)).center).x, C = i.y, w = i.z;
        return i.x = w, i.y = v, i.z = C, n
    }, D.isOccluded = function (e, t) {
        return !t.isBoundingSphereVisible(e)
    }, D.equals = function (e, t) {
        return e === t || I.defined(e) && I.defined(t) && P.Cartesian3.equals(e.center, t.center) && e.radius === t.radius
    }, D.prototype.intersectPlane = function (e) {
        return D.intersectPlane(this, e)
    }, D.prototype.distanceSquaredTo = function (e) {
        return D.distanceSquaredTo(this, e)
    }, D.prototype.computePlaneDistances = function (e, t, n) {
        return D.computePlaneDistances(this, e, t, n)
    }, D.prototype.isOccluded = function (e) {
        return D.isOccluded(this, e)
    }, D.prototype.equals = function (e) {
        return D.equals(this, e)
    }, D.prototype.clone = function (e) {
        return D.clone(this, e)
    }, D.prototype.volume = function () {
        var e = this.radius;
        return b * e * e * e
    };
    var Ee, Oe, _e, Se, be, Me, Re, Ae, Te, qe, ze, Ie, Pe, De, Ne, Ue, Fe, Ve = {
        requestFullscreen: void 0,
        exitFullscreen: void 0,
        fullscreenEnabled: void 0,
        fullscreenElement: void 0,
        fullscreenchange: void 0,
        fullscreenerror: void 0
    }, Le = {};

    function Be(e) {
        for (var t = e.split("."), n = 0, r = t.length; n < r; ++n) t[n] = parseInt(t[n], 10);
        return t
    }

    function We() {
        if (!I.defined(Oe) && (Oe = !1, !Ze())) {
            var e = / Chrome\/([\.0-9]+)/.exec(Ee.userAgent);
            null !== e && (Oe = !0, _e = Be(e[1]))
        }
        return Oe
    }

    function je() {
        if (!I.defined(Se) && (Se = !1, !We() && !Ze() && / Safari\/[\.0-9]+/.test(Ee.userAgent))) {
            var e = / Version\/([\.0-9]+)/.exec(Ee.userAgent);
            null !== e && (Se = !0, be = Be(e[1]))
        }
        return Se
    }

    function ke() {
        if (!I.defined(Me)) {
            Me = !1;
            var e = / AppleWebKit\/([\.0-9]+)(\+?)/.exec(Ee.userAgent);
            null !== e && (Me = !0, (Re = Be(e[1])).isNightly = !!e[2])
        }
        return Me
    }

    function Ye() {
        var e;
        I.defined(Ae) || (Ae = !1, "Microsoft Internet Explorer" === Ee.appName ? null !== (e = /MSIE ([0-9]{1,}[\.0-9]{0,})/.exec(Ee.userAgent)) && (Ae = !0, Te = Be(e[1])) : "Netscape" === Ee.appName && null !== (e = /Trident\/.*rv:([0-9]{1,}[\.0-9]{0,})/.exec(Ee.userAgent)) && (Ae = !0, Te = Be(e[1])));
        return Ae
    }

    function Ze() {
        if (!I.defined(qe)) {
            qe = !1;
            var e = / Edge\/([\.0-9]+)/.exec(Ee.userAgent);
            null !== e && (qe = !0, ze = Be(e[1]))
        }
        return qe
    }

    function Xe() {
        if (!I.defined(Ie)) {
            Ie = !1;
            var e = /Firefox\/([\.0-9]+)/.exec(Ee.userAgent);
            null !== e && (Ie = !0, Pe = Be(e[1]))
        }
        return Ie
    }

    function He() {
        if (!I.defined(Fe)) {
            var e = document.createElement("canvas");
            e.setAttribute("style", "image-rendering: -moz-crisp-edges;image-rendering: pixelated;");
            var t = e.style.imageRendering;
            (Fe = I.defined(t) && "" !== t) && (Ue = t)
        }
        return Fe
    }

    function Je() {
        return Je._result
    }

    Object.defineProperties(Le, {
        element: {
            get: function () {
                if (Le.supportsFullscreen()) return document[Ve.fullscreenElement]
            }
        }, changeEventName: {
            get: function () {
                if (Le.supportsFullscreen()) return Ve.fullscreenchange
            }
        }, errorEventName: {
            get: function () {
                if (Le.supportsFullscreen()) return Ve.fullscreenerror
            }
        }, enabled: {
            get: function () {
                if (Le.supportsFullscreen()) return document[Ve.fullscreenEnabled]
            }
        }, fullscreen: {
            get: function () {
                if (Le.supportsFullscreen()) return null !== Le.element
            }
        }
    }), Le.supportsFullscreen = function () {
        if (I.defined(ge)) return ge;
        ge = !1;
        var e = document.body;
        if ("function" == typeof e.requestFullscreen) return Ve.requestFullscreen = "requestFullscreen", Ve.exitFullscreen = "exitFullscreen", Ve.fullscreenEnabled = "fullscreenEnabled", Ve.fullscreenElement = "fullscreenElement", Ve.fullscreenchange = "fullscreenchange", Ve.fullscreenerror = "fullscreenerror", ge = !0;
        for (var t, n = ["webkit", "moz", "o", "ms", "khtml"], r = 0, a = n.length; r < a; ++r) {
            var i = n[r];
            "function" != typeof e[t = i + "RequestFullscreen"] && "function" != typeof e[t = i + "RequestFullScreen"] || (Ve.requestFullscreen = t, ge = !0), t = i + "ExitFullscreen", "function" == typeof document[t] ? Ve.exitFullscreen = t : (t = i + "CancelFullScreen", "function" == typeof document[t] && (Ve.exitFullscreen = t)), t = i + "FullscreenEnabled", void 0 !== document[t] ? Ve.fullscreenEnabled = t : (t = i + "FullScreenEnabled", void 0 !== document[t] && (Ve.fullscreenEnabled = t)), t = i + "FullscreenElement", void 0 !== document[t] ? Ve.fullscreenElement = t : (t = i + "FullScreenElement", void 0 !== document[t] && (Ve.fullscreenElement = t)), t = i + "fullscreenchange", void 0 !== document["on" + t] && ("ms" === i && (t = "MSFullscreenChange"), Ve.fullscreenchange = t), t = i + "fullscreenerror", void 0 !== document["on" + t] && ("ms" === i && (t = "MSFullscreenError"), Ve.fullscreenerror = t)
        }
        return ge
    }, Le.requestFullscreen = function (e, t) {
        Le.supportsFullscreen() && e[Ve.requestFullscreen]({vrDisplay: t})
    }, Le.exitFullscreen = function () {
        Le.supportsFullscreen() && document[Ve.exitFullscreen]()
    }, Le._names = Ve, Ee = "undefined" != typeof navigator ? navigator : {}, Je._promise = void 0, Je._result = void 0, Je.initialize = function () {
        if (I.defined(Je._promise)) return Je._promise;
        var e = I.when.defer();
        if (Je._promise = e.promise, Ze()) return Je._result = !1, e.resolve(Je._result), e.promise;
        var t = new Image;
        return t.onload = function () {
            Je._result = 0 < t.width && 0 < t.height, e.resolve(Je._result)
        }, t.onerror = function () {
            Je._result = !1, e.resolve(Je._result)
        }, t.src = "data:image/webp;base64,UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA", e.promise
    }, Object.defineProperties(Je, {
        initialized: {
            get: function () {
                return I.defined(Je._result)
            }
        }
    });
    var Ge = [];
    "undefined" != typeof ArrayBuffer && (Ge.push(Int8Array, Uint8Array, Int16Array, Uint16Array, Int32Array, Uint32Array, Float32Array, Float64Array), "undefined" != typeof Uint8ClampedArray && Ge.push(Uint8ClampedArray), "undefined" != typeof CanvasPixelArray && Ge.push(CanvasPixelArray));
    var Qe = {
        isChrome: We, chromeVersion: function () {
            return We() && _e
        }, isSafari: je, safariVersion: function () {
            return je() && be
        }, isWebkit: ke, webkitVersion: function () {
            return ke() && Re
        }, isInternetExplorer: Ye, internetExplorerVersion: function () {
            return Ye() && Te
        }, isEdge: Ze, edgeVersion: function () {
            return Ze() && ze
        }, isFirefox: Xe, firefoxVersion: function () {
            return Xe() && Pe
        }, isWindows: function () {
            return I.defined(De) || (De = /Windows/i.test(Ee.appVersion)), De
        }, hardwareConcurrency: I.defaultValue(Ee.hardwareConcurrency, 3), supportsPointerEvents: function () {
            return I.defined(Ne) || (Ne = !Xe() && "undefined" != typeof PointerEvent && (!I.defined(Ee.pointerEnabled) || Ee.pointerEnabled)), Ne
        }, supportsImageRenderingPixelated: He, supportsWebP: Je, imageRenderingValue: function () {
            return He() ? Ue : void 0
        }, typedArrayTypes: Ge
    };

    function Ke(e, t, n, r) {
        this.x = I.defaultValue(e, 0), this.y = I.defaultValue(t, 0), this.z = I.defaultValue(n, 0), this.w = I.defaultValue(r, 0)
    }

    Qe.supportsFullscreen = function () {
        return Le.supportsFullscreen()
    }, Qe.supportsTypedArrays = function () {
        return "undefined" != typeof ArrayBuffer
    }, Qe.supportsWebWorkers = function () {
        return "undefined" != typeof Worker
    }, Qe.supportsWebAssembly = function () {
        return "undefined" != typeof WebAssembly && !Qe.isEdge()
    };
    var $e = new P.Cartesian3;
    Ke.fromAxisAngle = function (e, t, n) {
        var r = t / 2, a = Math.sin(r), i = ($e = P.Cartesian3.normalize(e, $e)).x * a, s = $e.y * a, o = $e.z * a,
            u = Math.cos(r);
        return I.defined(n) ? (n.x = i, n.y = s, n.z = o, n.w = u, n) : new Ke(i, s, o, u)
    };
    var et = [1, 2, 0], tt = new Array(3);
    Ke.fromRotationMatrix = function (e, t) {
        var n, r, a, i, s, o = e[J.COLUMN0ROW0], u = e[J.COLUMN1ROW1], l = e[J.COLUMN2ROW2], c = o + u + l;
        if (0 < c) s = .5 * (n = Math.sqrt(c + 1)), n = .5 / n, r = (e[J.COLUMN1ROW2] - e[J.COLUMN2ROW1]) * n, a = (e[J.COLUMN2ROW0] - e[J.COLUMN0ROW2]) * n, i = (e[J.COLUMN0ROW1] - e[J.COLUMN1ROW0]) * n; else {
            var d = 0;
            o < u && (d = 1), o < l && u < l && (d = 2);
            var f = et[d], h = et[f];
            n = Math.sqrt(e[J.getElementIndex(d, d)] - e[J.getElementIndex(f, f)] - e[J.getElementIndex(h, h)] + 1);
            var p = tt;
            p[d] = .5 * n, n = .5 / n, s = (e[J.getElementIndex(h, f)] - e[J.getElementIndex(f, h)]) * n, p[f] = (e[J.getElementIndex(f, d)] + e[J.getElementIndex(d, f)]) * n, p[h] = (e[J.getElementIndex(h, d)] + e[J.getElementIndex(d, h)]) * n, r = -p[0], a = -p[1], i = -p[2]
        }
        return I.defined(t) ? (t.x = r, t.y = a, t.z = i, t.w = s, t) : new Ke(r, a, i, s)
    };
    var nt = new Ke, rt = new Ke, at = new Ke, it = new Ke;
    Ke.fromHeadingPitchRoll = function (e, t) {
        return it = Ke.fromAxisAngle(P.Cartesian3.UNIT_X, e.roll, nt), at = Ke.fromAxisAngle(P.Cartesian3.UNIT_Y, -e.pitch, t), t = Ke.multiply(at, it, at), rt = Ke.fromAxisAngle(P.Cartesian3.UNIT_Z, -e.heading, nt), Ke.multiply(rt, t, t)
    };
    var st = new P.Cartesian3, ot = new P.Cartesian3, ut = new Ke, lt = new Ke, ct = new Ke;
    Ke.packedLength = 4, Ke.pack = function (e, t, n) {
        return n = I.defaultValue(n, 0), t[n++] = e.x, t[n++] = e.y, t[n++] = e.z, t[n] = e.w, t
    }, Ke.unpack = function (e, t, n) {
        return t = I.defaultValue(t, 0), I.defined(n) || (n = new Ke), n.x = e[t], n.y = e[t + 1], n.z = e[t + 2], n.w = e[t + 3], n
    }, Ke.packedInterpolationLength = 3, Ke.convertPackedArrayForInterpolation = function (e, t, n, r) {
        Ke.unpack(e, 4 * n, ct), Ke.conjugate(ct, ct);
        for (var a = 0, i = n - t + 1; a < i; a++) {
            var s = 3 * a;
            Ke.unpack(e, 4 * (t + a), ut), Ke.multiply(ut, ct, ut), ut.w < 0 && Ke.negate(ut, ut), Ke.computeAxis(ut, st);
            var o = Ke.computeAngle(ut);
            r[s] = st.x * o, r[1 + s] = st.y * o, r[2 + s] = st.z * o
        }
    }, Ke.unpackInterpolationResult = function (e, t, n, r, a) {
        I.defined(a) || (a = new Ke), P.Cartesian3.fromArray(e, 0, ot);
        var i = P.Cartesian3.magnitude(ot);
        return Ke.unpack(t, 4 * r, lt), 0 === i ? Ke.clone(Ke.IDENTITY, ut) : Ke.fromAxisAngle(ot, i, ut), Ke.multiply(ut, lt, a)
    }, Ke.clone = function (e, t) {
        if (I.defined(e)) return I.defined(t) ? (t.x = e.x, t.y = e.y, t.z = e.z, t.w = e.w, t) : new Ke(e.x, e.y, e.z, e.w)
    }, Ke.conjugate = function (e, t) {
        return t.x = -e.x, t.y = -e.y, t.z = -e.z, t.w = e.w, t
    }, Ke.magnitudeSquared = function (e) {
        return e.x * e.x + e.y * e.y + e.z * e.z + e.w * e.w
    }, Ke.magnitude = function (e) {
        return Math.sqrt(Ke.magnitudeSquared(e))
    }, Ke.normalize = function (e, t) {
        var n = 1 / Ke.magnitude(e), r = e.x * n, a = e.y * n, i = e.z * n, s = e.w * n;
        return t.x = r, t.y = a, t.z = i, t.w = s, t
    }, Ke.inverse = function (e, t) {
        var n = Ke.magnitudeSquared(e);
        return t = Ke.conjugate(e, t), Ke.multiplyByScalar(t, 1 / n, t)
    }, Ke.add = function (e, t, n) {
        return n.x = e.x + t.x, n.y = e.y + t.y, n.z = e.z + t.z, n.w = e.w + t.w, n
    }, Ke.subtract = function (e, t, n) {
        return n.x = e.x - t.x, n.y = e.y - t.y, n.z = e.z - t.z, n.w = e.w - t.w, n
    }, Ke.negate = function (e, t) {
        return t.x = -e.x, t.y = -e.y, t.z = -e.z, t.w = -e.w, t
    }, Ke.dot = function (e, t) {
        return e.x * t.x + e.y * t.y + e.z * t.z + e.w * t.w
    }, Ke.multiply = function (e, t, n) {
        var r = e.x, a = e.y, i = e.z, s = e.w, o = t.x, u = t.y, l = t.z, c = t.w, d = s * o + r * c + a * l - i * u,
            f = s * u - r * l + a * c + i * o, h = s * l + r * u - a * o + i * c, p = s * c - r * o - a * u - i * l;
        return n.x = d, n.y = f, n.z = h, n.w = p, n
    }, Ke.multiplyByScalar = function (e, t, n) {
        return n.x = e.x * t, n.y = e.y * t, n.z = e.z * t, n.w = e.w * t, n
    }, Ke.divideByScalar = function (e, t, n) {
        return n.x = e.x / t, n.y = e.y / t, n.z = e.z / t, n.w = e.w / t, n
    }, Ke.computeAxis = function (e, t) {
        var n = e.w;
        if (Math.abs(n - 1) < X.CesiumMath.EPSILON6) return t.x = t.y = t.z = 0, t;
        var r = 1 / Math.sqrt(1 - n * n);
        return t.x = e.x * r, t.y = e.y * r, t.z = e.z * r, t
    }, Ke.computeAngle = function (e) {
        return Math.abs(e.w - 1) < X.CesiumMath.EPSILON6 ? 0 : 2 * Math.acos(e.w)
    };
    var dt = new Ke;
    Ke.lerp = function (e, t, n, r) {
        return dt = Ke.multiplyByScalar(t, n, dt), r = Ke.multiplyByScalar(e, 1 - n, r), Ke.add(dt, r, r)
    };
    var ft = new Ke, ht = new Ke, pt = new Ke;
    Ke.slerp = function (e, t, n, r) {
        var a = Ke.dot(e, t), i = t;
        if (a < 0 && (a = -a, i = ft = Ke.negate(t, ft)), 1 - a < X.CesiumMath.EPSILON6) return Ke.lerp(e, i, n, r);
        var s = Math.acos(a);
        return ht = Ke.multiplyByScalar(e, Math.sin((1 - n) * s), ht), pt = Ke.multiplyByScalar(i, Math.sin(n * s), pt), r = Ke.add(ht, pt, r), Ke.multiplyByScalar(r, 1 / Math.sin(s), r)
    }, Ke.log = function (e, t) {
        var n = X.CesiumMath.acosClamped(e.w), r = 0;
        return 0 !== n && (r = n / Math.sin(n)), P.Cartesian3.multiplyByScalar(e, r, t)
    }, Ke.exp = function (e, t) {
        var n = P.Cartesian3.magnitude(e), r = 0;
        return 0 !== n && (r = Math.sin(n) / n), t.x = e.x * r, t.y = e.y * r, t.z = e.z * r, t.w = Math.cos(n), t
    };
    var mt = new P.Cartesian3, yt = new P.Cartesian3, vt = new Ke, Ct = new Ke;
    Ke.computeInnerQuadrangle = function (e, t, n, r) {
        var a = Ke.conjugate(t, vt);
        Ke.multiply(a, n, Ct);
        var i = Ke.log(Ct, mt);
        Ke.multiply(a, e, Ct);
        var s = Ke.log(Ct, yt);
        return P.Cartesian3.add(i, s, i), P.Cartesian3.multiplyByScalar(i, .25, i), P.Cartesian3.negate(i, i), Ke.exp(i, vt), Ke.multiply(t, vt, r)
    }, Ke.squad = function (e, t, n, r, a, i) {
        var s = Ke.slerp(e, t, a, vt), o = Ke.slerp(n, r, a, Ct);
        return Ke.slerp(s, o, 2 * a * (1 - a), i)
    };
    for (var wt = new Ke, gt = 1.9011074535173003, xt = Qe.supportsTypedArrays() ? new Float32Array(8) : [], Et = Qe.supportsTypedArrays() ? new Float32Array(8) : [], Ot = Qe.supportsTypedArrays() ? new Float32Array(8) : [], _t = Qe.supportsTypedArrays() ? new Float32Array(8) : [], St = 0; St < 7; ++St) {
        var bt = St + 1, Mt = 2 * bt + 1;
        xt[St] = 1 / (bt * Mt), Et[St] = bt / Mt
    }

    function Rt(e, t, n) {
        for (var r, a, i = 0, s = e.length - 1; i <= s;) if ((a = n(e[r = ~~((i + s) / 2)], t)) < 0) i = 1 + r; else {
            if (!(0 < a)) return r;
            s = r - 1
        }
        return ~(s + 1)
    }

    function At(e, t, n, r, a) {
        this.xPoleWander = e, this.yPoleWander = t, this.xPoleOffset = n, this.yPoleOffset = r, this.ut1MinusUtc = a
    }

    function Tt() {
        function w(e, t, n, r) {
            n = n || " ";
            var a = e.length >= t ? "" : Array(1 + t - e.length >>> 0).join(n);
            return r ? e + a : a + e
        }

        function g(e, t, n, r, a, i) {
            var s = r - e.length;
            return 0 < s && (e = n || !a ? w(e, r, i, n) : e.slice(0, t.length) + w("", s, "0", !0) + e.slice(t.length)), e
        }

        function x(e, t, n, r, a, i, s) {
            var o = e >>> 0;
            return e = (n = n && o && {
                2: "0b",
                8: "0",
                16: "0x"
            }[t] || "") + w(o.toString(t), i || 0, "0", !1), g(e, n, r, a, s)
        }

        function E(e, t, n, r, a, i) {
            return null != r && (e = e.slice(0, r)), g(e, "", t, n, a, i)
        }

        var O = arguments, _ = 0, e = O[_++];
        return e.replace(/%%|%(\d+\$)?([-+\'#0 ]*)(\*\d+\$|\*|\d+)?(\.(\*\d+\$|\*|\d+))?([scboxXuideEfFgG])/g, function (e, t, n, r, a, i, s) {
            var o, u, l, c, d;
            if ("%%" == e) return "%";
            for (var f = !1, h = "", p = !1, m = !1, y = " ", v = n.length, C = 0; n && C < v; C++) switch (n.charAt(C)) {
                case" ":
                    h = " ";
                    break;
                case"+":
                    h = "+";
                    break;
                case"-":
                    f = !0;
                    break;
                case"'":
                    y = n.charAt(C + 1);
                    break;
                case"0":
                    p = !0;
                    break;
                case"#":
                    m = !0
            }
            if ((r = r ? "*" == r ? +O[_++] : "*" == r.charAt(0) ? +O[r.slice(1, -1)] : +r : 0) < 0 && (r = -r, f = !0), !isFinite(r)) throw new Error("sprintf: (minimum-)width must be finite");
            switch (i = i ? "*" == i ? +O[_++] : "*" == i.charAt(0) ? +O[i.slice(1, -1)] : +i : -1 < "fFeE".indexOf(s) ? 6 : "d" == s ? 0 : void 0, d = t ? O[t.slice(0, -1)] : O[_++], s) {
                case"s":
                    return E(String(d), f, r, i, p, y);
                case"c":
                    return E(String.fromCharCode(+d), f, r, i, p);
                case"b":
                    return x(d, 2, m, f, r, i, p);
                case"o":
                    return x(d, 8, m, f, r, i, p);
                case"x":
                    return x(d, 16, m, f, r, i, p);
                case"X":
                    return x(d, 16, m, f, r, i, p).toUpperCase();
                case"u":
                    return x(d, 10, m, f, r, i, p);
                case"i":
                case"d":
                    return o = +d || 0, d = (u = (o = Math.round(o - o % 1)) < 0 ? "-" : h) + w(String(Math.abs(o)), i, "0", !1), g(d, u, f, r, p);
                case"e":
                case"E":
                case"f":
                case"F":
                case"g":
                case"G":
                    return u = (o = +d) < 0 ? "-" : h, l = ["toExponential", "toFixed", "toPrecision"]["efg".indexOf(s.toLowerCase())], c = ["toString", "toUpperCase"]["eEfFgG".indexOf(s) % 2], d = u + Math.abs(o)[l](i), g(d, u, f, r, p)[c]();
                default:
                    return e
            }
        })
    }

    function qt(e, t, n, r, a, i, s, o) {
        this.year = e, this.month = t, this.day = n, this.hour = r, this.minute = a, this.second = i, this.millisecond = s, this.isLeapSecond = o
    }

    function zt(e) {
        return e % 4 == 0 && e % 100 != 0 || e % 400 == 0
    }

    function It(e, t) {
        this.julianDate = e, this.offset = t
    }

    xt[7] = gt / 136, Et[7] = 8 * gt / 17, Ke.fastSlerp = function (e, t, n, r) {
        var a, i = Ke.dot(e, t);
        0 <= i ? a = 1 : (a = -1, i = -i);
        for (var s = i - 1, o = 1 - n, u = n * n, l = o * o, c = 7; 0 <= c; --c) Ot[c] = (xt[c] * u - Et[c]) * s, _t[c] = (xt[c] * l - Et[c]) * s;
        var d = a * n * (1 + Ot[0] * (1 + Ot[1] * (1 + Ot[2] * (1 + Ot[3] * (1 + Ot[4] * (1 + Ot[5] * (1 + Ot[6] * (1 + Ot[7])))))))),
            f = o * (1 + _t[0] * (1 + _t[1] * (1 + _t[2] * (1 + _t[3] * (1 + _t[4] * (1 + _t[5] * (1 + _t[6] * (1 + _t[7])))))))),
            h = Ke.multiplyByScalar(e, f, wt);
        return Ke.multiplyByScalar(t, d, r), Ke.add(h, r, r)
    }, Ke.fastSquad = function (e, t, n, r, a, i) {
        var s = Ke.fastSlerp(e, t, a, vt), o = Ke.fastSlerp(n, r, a, Ct);
        return Ke.fastSlerp(s, o, 2 * a * (1 - a), i)
    }, Ke.equals = function (e, t) {
        return e === t || I.defined(e) && I.defined(t) && e.x === t.x && e.y === t.y && e.z === t.z && e.w === t.w
    }, Ke.equalsEpsilon = function (e, t, n) {
        return e === t || I.defined(e) && I.defined(t) && Math.abs(e.x - t.x) <= n && Math.abs(e.y - t.y) <= n && Math.abs(e.z - t.z) <= n && Math.abs(e.w - t.w) <= n
    }, Ke.ZERO = Object.freeze(new Ke(0, 0, 0, 0)), Ke.IDENTITY = Object.freeze(new Ke(0, 0, 0, 1)), Ke.prototype.clone = function (e) {
        return Ke.clone(this, e)
    }, Ke.prototype.equals = function (e) {
        return Ke.equals(this, e)
    }, Ke.prototype.equalsEpsilon = function (e, t) {
        return Ke.equalsEpsilon(this, e, t)
    }, Ke.prototype.toString = function () {
        return "(" + this.x + ", " + this.y + ", " + this.z + ", " + this.w + ")"
    };
    var Pt = Object.freeze({
        SECONDS_PER_MILLISECOND: .001,
        SECONDS_PER_MINUTE: 60,
        MINUTES_PER_HOUR: 60,
        HOURS_PER_DAY: 24,
        SECONDS_PER_HOUR: 3600,
        MINUTES_PER_DAY: 1440,
        SECONDS_PER_DAY: 86400,
        DAYS_PER_JULIAN_CENTURY: 36525,
        PICOSECOND: 1e-9,
        MODIFIED_JULIAN_DATE_DIFFERENCE: 2400000.5
    }), Dt = Object.freeze({UTC: 0, TAI: 1}), Nt = new qt, Ut = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

    function Ft(e, t) {
        return $t.compare(e.julianDate, t.julianDate)
    }

    var Vt = new It;

    function Lt(e) {
        Vt.julianDate = e;
        var t = $t.leapSeconds, n = Rt(t, Vt, Ft);
        n < 0 && (n = ~n), n >= t.length && (n = t.length - 1);
        var r = t[n].offset;
        0 < n && r < $t.secondsDifference(t[n].julianDate, e) && (r = t[--n].offset);
        $t.addSeconds(e, r, e)
    }

    function Bt(e, t) {
        Vt.julianDate = e;
        var n = $t.leapSeconds, r = Rt(n, Vt, Ft);
        if (r < 0 && (r = ~r), 0 === r) return $t.addSeconds(e, -n[0].offset, t);
        if (r >= n.length) return $t.addSeconds(e, -n[r - 1].offset, t);
        var a = $t.secondsDifference(n[r].julianDate, e);
        return 0 === a ? $t.addSeconds(e, -n[r].offset, t) : a <= 1 ? void 0 : $t.addSeconds(e, -n[--r].offset, t)
    }

    function Wt(e, t, n) {
        var r = t / Pt.SECONDS_PER_DAY | 0;
        return e += r, (t -= Pt.SECONDS_PER_DAY * r) < 0 && (e--, t += Pt.SECONDS_PER_DAY), n.dayNumber = e, n.secondsOfDay = t, n
    }

    function jt(e, t, n, r, a, i, s) {
        var o = (t - 14) / 12 | 0, u = e + 4800 + o,
            l = (1461 * u / 4 | 0) + (367 * (t - 2 - 12 * o) / 12 | 0) - (3 * ((u + 100) / 100 | 0) / 4 | 0) + n - 32075;
        (r -= 12) < 0 && (r += 24);
        var c = i + (r * Pt.SECONDS_PER_HOUR + a * Pt.SECONDS_PER_MINUTE + s * Pt.SECONDS_PER_MILLISECOND);
        return 43200 <= c && --l, [l, c]
    }

    var kt = /^(\d{4})$/, Yt = /^(\d{4})-(\d{2})$/, Zt = /^(\d{4})-?(\d{3})$/, Xt = /^(\d{4})-?W(\d{2})-?(\d{1})?$/,
        Ht = /^(\d{4})-?(\d{2})-?(\d{2})$/, Jt = /([Z+\-])?(\d{2})?:?(\d{2})?$/,
        Gt = /^(\d{2})(\.\d+)?/.source + Jt.source, Qt = /^(\d{2}):?(\d{2})(\.\d+)?/.source + Jt.source,
        Kt = /^(\d{2}):?(\d{2}):?(\d{2})(\.\d+)?/.source + Jt.source;

    function $t(e, t, n) {
        this.dayNumber = void 0, this.secondsOfDay = void 0, e = I.defaultValue(e, 0), t = I.defaultValue(t, 0), n = I.defaultValue(n, Dt.UTC);
        var r = 0 | e;
        Wt(r, t += (e - r) * Pt.SECONDS_PER_DAY, this), n === Dt.UTC && Lt(this)
    }

    $t.fromGregorianDate = function (e, t) {
        var n = jt(e.year, e.month, e.day, e.hour, e.minute, e.second, e.millisecond);
        return I.defined(t) ? (Wt(n[0], n[1], t), Lt(t), t) : new $t(n[0], n[1], Dt.UTC)
    }, $t.fromDate = function (e, t) {
        var n = jt(e.getUTCFullYear(), e.getUTCMonth() + 1, e.getUTCDate(), e.getUTCHours(), e.getUTCMinutes(), e.getUTCSeconds(), e.getUTCMilliseconds());
        return I.defined(t) ? (Wt(n[0], n[1], t), Lt(t), t) : new $t(n[0], n[1], Dt.UTC)
    }, $t.fromIso8601 = function (e, t) {
        var n, r, a, i, s = (e = e.replace(",", ".")).split("T"), o = 1, u = 1, l = 0, c = 0, d = 0, f = 0, h = s[0],
            p = s[1];
        if (null !== (s = h.match(Ht))) n = +s[1], o = +s[2], u = +s[3]; else if (null !== (s = h.match(Yt))) n = +s[1], o = +s[2]; else if (null !== (s = h.match(kt))) n = +s[1]; else {
            var m;
            if (null !== (s = h.match(Zt))) n = +s[1], m = +s[2], a = zt(n); else if (null !== (s = h.match(Xt))) n = +s[1], m = 7 * +s[2] + (+s[3] || 0) - new Date(Date.UTC(n, 0, 4)).getUTCDay() - 3;
            (r = new Date(Date.UTC(n, 0, 1))).setUTCDate(m), o = r.getUTCMonth() + 1, u = r.getUTCDate()
        }
        if (a = zt(n), I.defined(p)) {
            null !== (s = p.match(Kt)) ? (l = +s[1], c = +s[2], d = +s[3], f = 1e3 * (s[4] || 0), i = 5) : null !== (s = p.match(Qt)) ? (l = +s[1], c = +s[2], d = 60 * (s[3] || 0), i = 4) : null !== (s = p.match(Gt)) && (l = +s[1], c = 60 * (s[2] || 0), i = 3);
            var y = s[i], v = +s[i + 1], C = +(s[i + 2] || 0);
            switch (y) {
                case"+":
                    l -= v, c -= C;
                    break;
                case"-":
                    l += v, c += C;
                    break;
                case"Z":
                    break;
                default:
                    c += new Date(Date.UTC(n, o - 1, u, l, c)).getTimezoneOffset()
            }
        }
        var w = 60 === d;
        for (w && d--; 60 <= c;) c -= 60, l++;
        for (; 24 <= l;) l -= 24, u++;
        for (r = a && 2 === o ? 29 : Ut[o - 1]; r < u;) u -= r, 12 < ++o && (o -= 12, n++), r = a && 2 === o ? 29 : Ut[o - 1];
        for (; c < 0;) c += 60, l--;
        for (; l < 0;) l += 24, u--;
        for (; u < 1;) --o < 1 && (o += 12, n--), u += r = a && 2 === o ? 29 : Ut[o - 1];
        var g = jt(n, o, u, l, c, d, f);
        return I.defined(t) ? (Wt(g[0], g[1], t), Lt(t)) : t = new $t(g[0], g[1], Dt.UTC), w && $t.addSeconds(t, 1, t), t
    }, $t.now = function (e) {
        return $t.fromDate(new Date, e)
    };
    var en = new $t(0, 0, Dt.TAI);

    function tn(e) {
        if (e instanceof tn) this.scheme = e.scheme, this.authority = e.authority, this.path = e.path, this.query = e.query, this.fragment = e.fragment; else if (e) {
            var t = nn.exec(e);
            this.scheme = t[1], this.authority = t[2], this.path = t[3], this.query = t[4], this.fragment = t[5]
        }
    }

    $t.toGregorianDate = function (e, t) {
        var n = !1, r = Bt(e, en);
        I.defined(r) || ($t.addSeconds(e, -1, en), r = Bt(en, en), n = !0);
        var a = r.dayNumber, i = r.secondsOfDay;
        43200 <= i && (a += 1);
        var s = a + 68569 | 0, o = 4 * s / 146097 | 0,
            u = 4e3 * ((s = s - ((146097 * o + 3) / 4 | 0) | 0) + 1) / 1461001 | 0,
            l = 80 * (s = s - (1461 * u / 4 | 0) + 31 | 0) / 2447 | 0, c = s - (2447 * l / 80 | 0) | 0,
            d = 2 + l - 12 * (s = l / 11 | 0) | 0, f = 100 * (o - 49) + u + s | 0, h = i / Pt.SECONDS_PER_HOUR | 0,
            p = i - h * Pt.SECONDS_PER_HOUR, m = p / Pt.SECONDS_PER_MINUTE | 0,
            y = 0 | (p -= m * Pt.SECONDS_PER_MINUTE), v = (p - y) / Pt.SECONDS_PER_MILLISECOND;
        return 23 < (h += 12) && (h -= 24), n && (y += 1), I.defined(t) ? (t.year = f, t.month = d, t.day = c, t.hour = h, t.minute = m, t.second = y, t.millisecond = v, t.isLeapSecond = n, t) : new qt(f, d, c, h, m, y, v, n)
    }, $t.toDate = function (e) {
        var t = $t.toGregorianDate(e, Nt), n = t.second;
        return t.isLeapSecond && --n, new Date(Date.UTC(t.year, t.month - 1, t.day, t.hour, t.minute, n, t.millisecond))
    }, $t.toIso8601 = function (e, t) {
        var n = $t.toGregorianDate(e, Nt), r = n.year, a = n.month, i = n.day, s = n.hour, o = n.minute, u = n.second,
            l = n.millisecond;
        return 1e4 === r && 1 === a && 1 === i && 0 === s && 0 === o && 0 === u && 0 === l && (r = 9999, a = 12, i = 31, s = 24), I.defined(t) || 0 === l ? I.defined(t) && 0 !== t ? Tt("%04d-%02d-%02dT%02d:%02d:%02d.%sZ", r, a, i, s, o, u, (.01 * l).toFixed(t).replace(".", "").slice(0, t)) : Tt("%04d-%02d-%02dT%02d:%02d:%02dZ", r, a, i, s, o, u) : Tt("%04d-%02d-%02dT%02d:%02d:%02d.%sZ", r, a, i, s, o, u, (.01 * l).toString().replace(".", ""))
    }, $t.clone = function (e, t) {
        if (I.defined(e)) return I.defined(t) ? (t.dayNumber = e.dayNumber, t.secondsOfDay = e.secondsOfDay, t) : new $t(e.dayNumber, e.secondsOfDay, Dt.TAI)
    }, $t.compare = function (e, t) {
        var n = e.dayNumber - t.dayNumber;
        return 0 != n ? n : e.secondsOfDay - t.secondsOfDay
    }, $t.equals = function (e, t) {
        return e === t || I.defined(e) && I.defined(t) && e.dayNumber === t.dayNumber && e.secondsOfDay === t.secondsOfDay
    }, $t.equalsEpsilon = function (e, t, n) {
        return e === t || I.defined(e) && I.defined(t) && Math.abs($t.secondsDifference(e, t)) <= n
    }, $t.totalDays = function (e) {
        return e.dayNumber + e.secondsOfDay / Pt.SECONDS_PER_DAY
    }, $t.secondsDifference = function (e, t) {
        return (e.dayNumber - t.dayNumber) * Pt.SECONDS_PER_DAY + (e.secondsOfDay - t.secondsOfDay)
    }, $t.daysDifference = function (e, t) {
        return e.dayNumber - t.dayNumber + (e.secondsOfDay - t.secondsOfDay) / Pt.SECONDS_PER_DAY
    }, $t.computeTaiMinusUtc = function (e) {
        Vt.julianDate = e;
        var t = $t.leapSeconds, n = Rt(t, Vt, Ft);
        return n < 0 && (n = ~n, --n < 0 && (n = 0)), t[n].offset
    }, $t.addSeconds = function (e, t, n) {
        return Wt(e.dayNumber, e.secondsOfDay + t, n)
    }, $t.addMinutes = function (e, t, n) {
        var r = e.secondsOfDay + t * Pt.SECONDS_PER_MINUTE;
        return Wt(e.dayNumber, r, n)
    }, $t.addHours = function (e, t, n) {
        var r = e.secondsOfDay + t * Pt.SECONDS_PER_HOUR;
        return Wt(e.dayNumber, r, n)
    }, $t.addDays = function (e, t, n) {
        return Wt(e.dayNumber + t, e.secondsOfDay, n)
    }, $t.lessThan = function (e, t) {
        return $t.compare(e, t) < 0
    }, $t.lessThanOrEquals = function (e, t) {
        return $t.compare(e, t) <= 0
    }, $t.greaterThan = function (e, t) {
        return 0 < $t.compare(e, t)
    }, $t.greaterThanOrEquals = function (e, t) {
        return 0 <= $t.compare(e, t)
    }, $t.prototype.clone = function (e) {
        return $t.clone(this, e)
    }, $t.prototype.equals = function (e) {
        return $t.equals(this, e)
    }, $t.prototype.equalsEpsilon = function (e, t) {
        return $t.equalsEpsilon(this, e, t)
    }, $t.prototype.toString = function () {
        return $t.toIso8601(this)
    }, $t.leapSeconds = [new It(new $t(2441317, 43210, Dt.TAI), 10), new It(new $t(2441499, 43211, Dt.TAI), 11), new It(new $t(2441683, 43212, Dt.TAI), 12), new It(new $t(2442048, 43213, Dt.TAI), 13), new It(new $t(2442413, 43214, Dt.TAI), 14), new It(new $t(2442778, 43215, Dt.TAI), 15), new It(new $t(2443144, 43216, Dt.TAI), 16), new It(new $t(2443509, 43217, Dt.TAI), 17), new It(new $t(2443874, 43218, Dt.TAI), 18), new It(new $t(2444239, 43219, Dt.TAI), 19), new It(new $t(2444786, 43220, Dt.TAI), 20), new It(new $t(2445151, 43221, Dt.TAI), 21), new It(new $t(2445516, 43222, Dt.TAI), 22), new It(new $t(2446247, 43223, Dt.TAI), 23), new It(new $t(2447161, 43224, Dt.TAI), 24), new It(new $t(2447892, 43225, Dt.TAI), 25), new It(new $t(2448257, 43226, Dt.TAI), 26), new It(new $t(2448804, 43227, Dt.TAI), 27), new It(new $t(2449169, 43228, Dt.TAI), 28), new It(new $t(2449534, 43229, Dt.TAI), 29), new It(new $t(2450083, 43230, Dt.TAI), 30), new It(new $t(2450630, 43231, Dt.TAI), 31), new It(new $t(2451179, 43232, Dt.TAI), 32), new It(new $t(2453736, 43233, Dt.TAI), 33), new It(new $t(2454832, 43234, Dt.TAI), 34), new It(new $t(2456109, 43235, Dt.TAI), 35), new It(new $t(2457204, 43236, Dt.TAI), 36), new It(new $t(2457754, 43237, Dt.TAI), 37)], tn.prototype.scheme = null, tn.prototype.authority = null, tn.prototype.path = "", tn.prototype.query = null, tn.prototype.fragment = null;
    var nn = new RegExp("^(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?$");
    tn.prototype.getScheme = function () {
        return this.scheme
    }, tn.prototype.getAuthority = function () {
        return this.authority
    }, tn.prototype.getPath = function () {
        return this.path
    }, tn.prototype.getQuery = function () {
        return this.query
    }, tn.prototype.getFragment = function () {
        return this.fragment
    }, tn.prototype.isAbsolute = function () {
        return !!this.scheme && !this.fragment
    }, tn.prototype.isSameDocumentAs = function (e) {
        return e.scheme == this.scheme && e.authority == this.authority && e.path == this.path && e.query == this.query
    }, tn.prototype.equals = function (e) {
        return this.isSameDocumentAs(e) && e.fragment == this.fragment
    }, tn.prototype.normalize = function () {
        this.removeDotSegments(), this.scheme && (this.scheme = this.scheme.toLowerCase()), this.authority && (this.authority = this.authority.replace(sn, un).replace(rn, on)), this.path && (this.path = this.path.replace(rn, on)), this.query && (this.query = this.query.replace(rn, on)), this.fragment && (this.fragment = this.fragment.replace(rn, on))
    };
    var rn = /%[0-9a-z]{2}/gi, an = /[a-zA-Z0-9\-\._~]/, sn = /(.*@)?([^@:]*)(:.*)?/;

    function on(e) {
        var t = unescape(e);
        return an.test(t) ? t : e.toUpperCase()
    }

    function un(e, t, n, r) {
        return (t || "") + n.toLowerCase() + (r || "")
    }

    function ln(e, t) {
        if (null === e || "object" != typeof e) return e;
        t = I.defaultValue(t, !1);
        var n = new e.constructor;
        for (var r in e) if (e.hasOwnProperty(r)) {
            var a = e[r];
            t && (a = ln(a, t)), n[r] = a
        }
        return n
    }

    function cn(e, t, n) {
        n = I.defaultValue(n, !1);
        var r, a, i, s = {}, o = I.defined(e), u = I.defined(t);
        if (o) for (r in e) e.hasOwnProperty(r) && (a = e[r], u && n && "object" == typeof a && t.hasOwnProperty(r) ? (i = t[r], s[r] = "object" == typeof i ? cn(a, i, n) : a) : s[r] = a);
        if (u) for (r in t) t.hasOwnProperty(r) && !s.hasOwnProperty(r) && (i = t[r], s[r] = i);
        return s
    }

    function dn(e, t) {
        var n;
        return "undefined" != typeof document && (n = document), dn._implementation(e, t, n)
    }

    tn.prototype.resolve = function (e) {
        var t = new tn;
        return this.scheme ? (t.scheme = this.scheme, t.authority = this.authority, t.path = this.path, t.query = this.query) : (t.scheme = e.scheme, this.authority ? (t.authority = this.authority, t.path = this.path, t.query = this.query) : (t.authority = e.authority, "" == this.path ? (t.path = e.path, t.query = this.query || e.query) : ("/" == this.path.charAt(0) ? t.path = this.path : e.authority && "" == e.path ? t.path = "/" + this.path : t.path = e.path.substring(0, e.path.lastIndexOf("/") + 1) + this.path, t.removeDotSegments(), t.query = this.query))), t.fragment = this.fragment, t
    }, tn.prototype.removeDotSegments = function () {
        var e, t = this.path.split("/"), n = [], r = "" == t[0];
        r && t.shift();
        for ("" == t[0] && t.shift(); t.length;) ".." == (e = t.shift()) ? n.pop() : "." != e && n.push(e);
        "." != e && ".." != e || n.push(""), r && n.unshift(""), this.path = n.join("/")
    }, tn.prototype.toString = function () {
        var e = "";
        return this.scheme && (e += this.scheme + ":"), this.authority && (e += "//" + this.authority), e += this.path, this.query && (e += "?" + this.query), this.fragment && (e += "#" + this.fragment), e
    }, dn._implementation = function (e, t, n) {
        if (!I.defined(t)) {
            if (void 0 === n) return e;
            t = I.defaultValue(n.baseURI, n.location.href)
        }
        var r = new tn(t);
        return new tn(e).resolve(r).toString()
    };
    var fn, hn = /^blob:/i;

    function pn(e) {
        return hn.test(e)
    }

    var mn = /^data:/i;

    function yn(e) {
        return mn.test(e)
    }

    var vn = Object.freeze({UNISSUED: 0, ISSUED: 1, ACTIVE: 2, RECEIVED: 3, CANCELLED: 4, FAILED: 5}),
        Cn = Object.freeze({TERRAIN: 0, IMAGERY: 1, TILES3D: 2, OTHER: 3});

    function wn(e) {
        e = I.defaultValue(e, I.defaultValue.EMPTY_OBJECT);
        var t = I.defaultValue(e.throttleByServer, !1), n = I.defaultValue(e.throttle, !1);
        this.url = e.url, this.requestFunction = e.requestFunction, this.cancelFunction = e.cancelFunction, this.priorityFunction = e.priorityFunction, this.priority = I.defaultValue(e.priority, 0), this.throttle = n, this.throttleByServer = t, this.type = I.defaultValue(e.type, Cn.OTHER), this.serverKey = void 0, this.state = vn.UNISSUED, this.deferred = void 0, this.cancelled = !1
    }

    function gn(e, t, n) {
        this.statusCode = e, this.response = t, this.responseHeaders = n, "string" == typeof this.responseHeaders && (this.responseHeaders = function (e) {
            var t = {};
            if (!e) return t;
            for (var n = e.split("\r\n"), r = 0; r < n.length; ++r) {
                var a = n[r], i = a.indexOf(": ");
                if (0 < i) {
                    var s = a.substring(0, i), o = a.substring(i + 2);
                    t[s] = o
                }
            }
            return t
        }(this.responseHeaders))
    }

    function xn() {
        this._listeners = [], this._scopes = [], this._toRemove = [], this._insideRaiseEvent = !1
    }

    function En(e, t) {
        return t - e
    }

    function On(e) {
        this._comparator = e.comparator, this._array = [], this._length = 0, this._maximumLength = void 0
    }

    function _n(e, t, n) {
        var r = e[t];
        e[t] = e[n], e[n] = r
    }

    wn.prototype.cancel = function () {
        this.cancelled = !0
    }, wn.prototype.clone = function (e) {
        return I.defined(e) ? (e.url = this.url, e.requestFunction = this.requestFunction, e.cancelFunction = this.cancelFunction, e.priorityFunction = this.priorityFunction, e.priority = this.priority, e.throttle = this.throttle, e.throttleByServer = this.throttleByServer, e.type = this.type, e.serverKey = this.serverKey, e.state = this.RequestState.UNISSUED, e.deferred = void 0, e.cancelled = !1, e) : new wn(this)
    }, gn.prototype.toString = function () {
        var e = "Request has failed.";
        return I.defined(this.statusCode) && (e += " Status Code: " + this.statusCode), e
    }, Object.defineProperties(xn.prototype, {
        numberOfListeners: {
            get: function () {
                return this._listeners.length - this._toRemove.length
            }
        }
    }), xn.prototype.addEventListener = function (e, t) {
        this._listeners.push(e), this._scopes.push(t);
        var n = this;
        return function () {
            n.removeEventListener(e, t)
        }
    }, xn.prototype.removeEventListener = function (e, t) {
        for (var n = this._listeners, r = this._scopes, a = -1, i = 0; i < n.length; i++) if (n[i] === e && r[i] === t) {
            a = i;
            break
        }
        return -1 !== a && (this._insideRaiseEvent ? (this._toRemove.push(a), n[a] = void 0, r[a] = void 0) : (n.splice(a, 1), r.splice(a, 1)), !0)
    }, xn.prototype.raiseEvent = function () {
        var e;
        this._insideRaiseEvent = !0;
        var t = this._listeners, n = this._scopes, r = t.length;
        for (e = 0; e < r; e++) {
            var a = t[e];
            I.defined(a) && t[e].apply(n[e], arguments)
        }
        var i = this._toRemove;
        if (0 < (r = i.length)) {
            for (i.sort(En), e = 0; e < r; e++) {
                var s = i[e];
                t.splice(s, 1), n.splice(s, 1)
            }
            i.length = 0
        }
        this._insideRaiseEvent = !1
    }, Object.defineProperties(On.prototype, {
        length: {
            get: function () {
                return this._length
            }
        }, internalArray: {
            get: function () {
                return this._array
            }
        }, maximumLength: {
            get: function () {
                return this._maximumLength
            }, set: function (e) {
                this._maximumLength = e, this._length > e && 0 < e && (this._length = e, this._array.length = e)
            }
        }, comparator: {
            get: function () {
                return this._comparator
            }
        }
    }), On.prototype.reserve = function (e) {
        e = I.defaultValue(e, this._length), this._array.length = e
    }, On.prototype.heapify = function (e) {
        e = I.defaultValue(e, 0);
        for (var t = this._length, n = this._comparator, r = this._array, a = -1, i = !0; i;) {
            var s = 2 * (e + 1), o = s - 1;
            a = o < t && n(r[o], r[e]) < 0 ? o : e, s < t && n(r[s], r[a]) < 0 && (a = s), a !== e ? (_n(r, a, e), e = a) : i = !1
        }
    }, On.prototype.resort = function () {
        for (var e = this._length, t = Math.ceil(e / 2); 0 <= t; --t) this.heapify(t)
    }, On.prototype.insert = function (e) {
        var t, n = this._array, r = this._comparator, a = this._maximumLength, i = this._length++;
        for (i < n.length ? n[i] = e : n.push(e); 0 !== i;) {
            var s = Math.floor((i - 1) / 2);
            if (!(r(n[i], n[s]) < 0)) break;
            _n(n, i, s), i = s
        }
        return I.defined(a) && this._length > a && (t = n[a], this._length = a), t
    }, On.prototype.pop = function (e) {
        if (e = I.defaultValue(e, 0), 0 !== this._length) {
            var t = this._array, n = t[e];
            return _n(t, e, --this._length), this.heapify(e), n
        }
    };
    var Sn = {
        numberOfAttemptedRequests: 0,
        numberOfActiveRequests: 0,
        numberOfCancelledRequests: 0,
        numberOfCancelledActiveRequests: 0,
        numberOfFailedRequests: 0,
        numberOfActiveRequestsEver: 0,
        lastNumberOfActiveRequests: 0
    }, bn = 20, Mn = new On({
        comparator: function (e, t) {
            return e.priority - t.priority
        }
    });
    Mn.maximumLength = bn, Mn.reserve(bn);
    var Rn = [], An = {}, Tn = "undefined" != typeof document ? new tn(document.location.href) : new tn, qn = new xn;

    function zn() {
    }

    function In(e) {
        I.defined(e.priorityFunction) && (e.priority = e.priorityFunction())
    }

    function Pn(e) {
        var t = I.defaultValue(zn.requestsByServer[e], zn.maximumRequestsPerServer);
        return An[e] < t
    }

    function Dn(e) {
        return e.state === vn.UNISSUED && (e.state = vn.ISSUED, e.deferred = I.when.defer()), e.deferred.promise
    }

    function Nn(e) {
        var t, n, r = Dn(e);
        return e.state = vn.ACTIVE, Rn.push(e), ++Sn.numberOfActiveRequests, ++Sn.numberOfActiveRequestsEver, ++An[e.serverKey], e.requestFunction().then((n = e, function (e) {
            n.state !== vn.CANCELLED && (--Sn.numberOfActiveRequests, --An[n.serverKey], qn.raiseEvent(), n.state = vn.RECEIVED, n.deferred.resolve(e))
        })).otherwise((t = e, function (e) {
            t.state !== vn.CANCELLED && (++Sn.numberOfFailedRequests, --Sn.numberOfActiveRequests, --An[t.serverKey], qn.raiseEvent(e), t.state = vn.FAILED, t.deferred.reject(e))
        })), r
    }

    function Un(e) {
        var t = e.state === vn.ACTIVE;
        e.state = vn.CANCELLED, ++Sn.numberOfCancelledRequests, e.deferred.reject(), t && (--Sn.numberOfActiveRequests, --An[e.serverKey], ++Sn.numberOfCancelledActiveRequests), I.defined(e.cancelFunction) && e.cancelFunction()
    }

    zn.maximumRequests = 50, zn.maximumRequestsPerServer = 6, zn.requestsByServer = {
        "api.cesium.com:443": 18,
        "assets.cesium.com:443": 18
    }, zn.throttleRequests = !0, zn.debugShowStatistics = !1, zn.requestCompletedEvent = qn, Object.defineProperties(zn, {
        statistics: {
            get: function () {
                return Sn
            }
        }, priorityHeapLength: {
            get: function () {
                return bn
            }, set: function (e) {
                if (e < bn) for (; Mn.length > e;) {
                    Un(Mn.pop())
                }
                bn = e, Mn.maximumLength = e, Mn.reserve(e)
            }
        }
    }), zn.update = function () {
        var e, t, n = 0, r = Rn.length;
        for (e = 0; e < r; ++e) (t = Rn[e]).cancelled && Un(t), t.state === vn.ACTIVE ? 0 < n && (Rn[e - n] = t) : ++n;
        Rn.length -= n;
        var a = Mn.internalArray, i = Mn.length;
        for (e = 0; e < i; ++e) In(a[e]);
        Mn.resort();
        for (var s = Math.max(zn.maximumRequests - Rn.length, 0), o = 0; o < s && 0 < Mn.length;) !(t = Mn.pop()).cancelled && (!t.throttleByServer || Pn(t.serverKey)) ? (Nn(t), ++o) : Un(t);
        !function () {
            if (!zn.debugShowStatistics) return;
            0 === Sn.numberOfActiveRequests && 0 < Sn.lastNumberOfActiveRequests && (0 < Sn.numberOfAttemptedRequests && (console.log("Number of attempted requests: " + Sn.numberOfAttemptedRequests), Sn.numberOfAttemptedRequests = 0), 0 < Sn.numberOfCancelledRequests && (console.log("Number of cancelled requests: " + Sn.numberOfCancelledRequests), Sn.numberOfCancelledRequests = 0), 0 < Sn.numberOfCancelledActiveRequests && (console.log("Number of cancelled active requests: " + Sn.numberOfCancelledActiveRequests), Sn.numberOfCancelledActiveRequests = 0), 0 < Sn.numberOfFailedRequests && (console.log("Number of failed requests: " + Sn.numberOfFailedRequests), Sn.numberOfFailedRequests = 0));
            Sn.lastNumberOfActiveRequests = Sn.numberOfActiveRequests
        }()
    }, zn.getServerKey = function (e) {
        var t = new tn(e).resolve(Tn);
        t.normalize();
        var n = t.authority;
        /:/.test(n) || (n = n + ":" + ("https" === t.scheme ? "443" : "80"));
        var r = An[n];
        return I.defined(r) || (An[n] = 0), n
    }, zn.request = function (e) {
        if (yn(e.url) || pn(e.url)) return qn.raiseEvent(), e.state = vn.RECEIVED, e.requestFunction();
        if (++Sn.numberOfAttemptedRequests, I.defined(e.serverKey) || (e.serverKey = zn.getServerKey(e.url)), !zn.throttleRequests || !e.throttleByServer || Pn(e.serverKey)) {
            if (!zn.throttleRequests || !e.throttle) return Nn(e);
            if (!(Rn.length >= zn.maximumRequests)) {
                In(e);
                var t = Mn.insert(e);
                if (I.defined(t)) {
                    if (t === e) return;
                    Un(t)
                }
                return Dn(e)
            }
        }
    }, zn.clearForSpecs = function () {
        for (; 0 < Mn.length;) {
            Un(Mn.pop())
        }
        for (var e = Rn.length, t = 0; t < e; ++t) Un(Rn[t]);
        Rn.length = 0, An = {}, Sn.numberOfAttemptedRequests = 0, Sn.numberOfActiveRequests = 0, Sn.numberOfCancelledRequests = 0, Sn.numberOfCancelledActiveRequests = 0, Sn.numberOfFailedRequests = 0, Sn.numberOfActiveRequestsEver = 0, Sn.lastNumberOfActiveRequests = 0
    }, zn.numberOfActiveRequestsByServer = function (e) {
        return An[e]
    }, zn.requestHeap = Mn;
    var Fn = {}, Vn = {};
    Fn.add = function (e, t) {
        var n = e.toLowerCase() + ":" + t;
        I.defined(Vn[n]) || (Vn[n] = !0)
    }, Fn.remove = function (e, t) {
        var n = e.toLowerCase() + ":" + t;
        I.defined(Vn[n]) && delete Vn[n]
    }, Fn.contains = function (e) {
        var t = function (e) {
            var t = new tn(e);
            t.normalize();
            var n = t.getAuthority();
            if (I.defined(n)) {
                if (-1 !== n.indexOf("@")) n = n.split("@")[1];
                if (-1 === n.indexOf(":")) {
                    var r = t.getScheme();
                    if (I.defined(r) || (r = (r = window.location.protocol).substring(0, r.length - 1)), "http" === r) n += ":80"; else {
                        if ("https" !== r) return;
                        n += ":443"
                    }
                }
                return n
            }
        }(e);
        return !(!I.defined(t) || !I.defined(Vn[t]))
    }, Fn.clear = function () {
        Vn = {}
    };
    var Ln, Bn = function () {
        try {
            var e = new XMLHttpRequest;
            return e.open("GET", "#", !0), (e.responseType = "blob") === e.responseType
        } catch (e) {
            return !1
        }
    }();

    function Wn(e, t, n, r) {
        var a, i = e.query;
        if (!I.defined(i) || 0 === i.length) return 1;
        if (-1 === i.indexOf("=")) {
            var s = {};
            s[i] = void 0, a = s
        } else a = function (e) {
            var t = {};
            if ("" === e) return t;
            for (var n = e.replace(/\+/g, "%20").split(/[&;]/), r = 0, a = n.length; r < a; ++r) {
                var i = n[r].split("="), s = decodeURIComponent(i[0]), o = i[1];
                o = I.defined(o) ? decodeURIComponent(o) : "";
                var u = t[s];
                "string" == typeof u ? t[s] = [u, o] : Array.isArray(u) ? u.push(o) : t[s] = o
            }
            return t
        }(i);
        t._formParameters = n ? Zn(a, t._formParameters, r) : a, e.query = void 0
    }

    function jn(e, t) {
        var n = t._formParameters, r = Object.keys(n);
        1 !== r.length || I.defined(n[r[0]]) ? e.query = function (e) {
            var t = "";
            for (var n in e) if (e.hasOwnProperty(n)) {
                var r = e[n], a = encodeURIComponent(n) + "=";
                if (Array.isArray(r)) for (var i = 0, s = r.length; i < s; ++i) t += a + encodeURIComponent(r[i]) + "&"; else t += a + encodeURIComponent(r) + "&"
            }
            return t = t.slice(0, -1)
        }(n) : e.query = r[0]
    }

    function kn(e, t) {
        return I.defined(e) ? I.defined(e.clone) ? e.clone() : ln(e) : t
    }

    function Yn(e) {
        if (e.state === vn.ISSUED || e.state === vn.ACTIVE) throw new H.RuntimeError("The Resource is already being fetched.");
        e.state = vn.UNISSUED, e.deferred = void 0
    }

    function Zn(e, t, n) {
        if (!n) return cn(e, t);
        var r = ln(e, !0);
        for (var a in t) if (t.hasOwnProperty(a)) {
            var i = r[a], s = t[a];
            I.defined(i) ? (Array.isArray(i) || (i = r[a] = [i]), r[a] = i.concat(s)) : r[a] = Array.isArray(s) ? s.slice() : s
        }
        return r
    }

    function Xn(e) {
        "string" == typeof (e = I.defaultValue(e, I.defaultValue.EMPTY_OBJECT)) && (e = {url: e}), this._url = void 0, this._templateValues = kn(e.templateValues, {}), this._formParameters = kn(e.formParameters, {}), this.headers = kn(e.headers, {}), this.request = I.defaultValue(e.request, new wn), this.proxy = e.proxy, this.retryCallback = e.retryCallback, this.retryAttempts = I.defaultValue(e.retryAttempts, 0), this._retryCount = 0;
        var t = new tn(e.url);
        Wn(t, this, !0, !0), t.fragment = void 0, this._url = t.toString()
    }

    function Hn(e) {
        var n = e.resource, r = e.flipY, a = e.preferImageBitmap, i = n.request;
        i.url = n.url, i.requestFunction = function () {
            var e = !1;
            n.isDataUri || n.isBlobUri || (e = n.isCrossOriginUrl);
            var t = I.when.defer();
            return Xn._Implementations.createImage(i, e, t, r, a), t.promise
        };
        var t = zn.request(i);
        if (I.defined(t)) return t.otherwise(function (t) {
            return i.state !== vn.FAILED ? I.when.reject(t) : n.retryOnError(t).then(function (e) {
                return e ? (i.state = vn.UNISSUED, i.deferred = void 0, Hn({
                    resource: n,
                    flipY: r,
                    preferImageBitmap: a
                })) : I.when.reject(t)
            })
        })
    }

    Xn.createIfNeeded = function (e) {
        return e instanceof Xn ? e.getDerivedResource({request: e.request}) : "string" != typeof e ? e : new Xn({url: e})
    }, Xn.supportsImageBitmapOptions = function () {
        if (I.defined(Ln)) return Ln;
        if ("function" != typeof createImageBitmap) return Ln = I.when.resolve(!1);
        return Ln = Xn.fetchBlob({url: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWP4////fwAJ+wP9CNHoHgAAAABJRU5ErkJggg=="}).then(function (e) {
            return createImageBitmap(e, {imageOrientation: "flipY", premultiplyAlpha: "none"})
        }).then(function (e) {
            return !0
        }).otherwise(function () {
            return !1
        })
    }, Object.defineProperties(Xn, {
        isBlobSupported: {
            get: function () {
                return Bn
            }
        }
    }), Object.defineProperties(Xn.prototype, {
        formParameters: {
            get: function () {
                return this._formParameters
            }
        }, templateValues: {
            get: function () {
                return this._templateValues
            }
        }, url: {
            get: function () {
                return this.getUrlComponent(!0, !0)
            }, set: function (e) {
                var t = new tn(e);
                Wn(t, this, !1), t.fragment = void 0, this._url = t.toString()
            }
        }, extension: {
            get: function () {
                return function (e) {
                    var t = new tn(e);
                    t.normalize();
                    var n = t.path, r = n.lastIndexOf("/");
                    return -1 !== r && (n = n.substr(r + 1)), n = -1 === (r = n.lastIndexOf(".")) ? "" : n.substr(r + 1)
                }(this._url)
            }
        }, isDataUri: {
            get: function () {
                return yn(this._url)
            }
        }, isBlobUri: {
            get: function () {
                return pn(this._url)
            }
        }, isCrossOriginUrl: {
            get: function () {
                return function (e) {
                    I.defined(fn) || (fn = document.createElement("a")), fn.href = window.location.href;
                    var t = fn.host, n = fn.protocol;
                    return fn.href = e, fn.href = fn.href, n !== fn.protocol || t !== fn.host
                }(this._url)
            }
        }, hasHeaders: {
            get: function () {
                return 0 < Object.keys(this.headers).length
            }
        }
    }), Xn.prototype.getUrlComponent = function (e, t) {
        if (this.isDataUri) return this._url;
        var n = new tn(this._url);
        e && jn(n, this);
        var r = n.toString().replace(/%7B/g, "{").replace(/%7D/g, "}"), a = this._templateValues;
        return r = r.replace(/{(.*?)}/g, function (e, t) {
            var n = a[t];
            return I.defined(n) ? encodeURIComponent(n) : e
        }), t && I.defined(this.proxy) && (r = this.proxy.getURL(r)), r
    }, Xn.prototype.setQueryParameters = function (e, t) {
        this._formParameters = t ? Zn(this._formParameters, e, !1) : Zn(e, this._formParameters, !1)
    }, Xn.prototype.appendQueryParameters = function (e) {
        this._formParameters = Zn(e, this._formParameters, !0)
    }, Xn.prototype.setTemplateValues = function (e, t) {
        this._templateValues = t ? cn(this._templateValues, e) : cn(e, this._templateValues)
    }, Xn.prototype.getDerivedResource = function (e) {
        var t = this.clone();
        if (t._retryCount = 0, I.defined(e.url)) {
            var n = new tn(e.url);
            Wn(n, t, !0, I.defaultValue(e.preserveQueryParameters, !1)), n.fragment = void 0, t._url = n.resolve(new tn(dn(this._url))).toString()
        }
        return I.defined(e.formParameters) && (t._formParameters = cn(e.formParameters, t._formParameters)), I.defined(e.templateValues) && (t._templateValues = cn(e.templateValues, t.templateValues)), I.defined(e.headers) && (t.headers = cn(e.headers, t.headers)), I.defined(e.proxy) && (t.proxy = e.proxy), I.defined(e.request) && (t.request = e.request), I.defined(e.retryCallback) && (t.retryCallback = e.retryCallback), I.defined(e.retryAttempts) && (t.retryAttempts = e.retryAttempts), t
    }, Xn.prototype.retryOnError = function (e) {
        var t = this.retryCallback;
        if ("function" != typeof t || this._retryCount >= this.retryAttempts) return I.when(!1);
        var n = this;
        return I.when(t(this, e)).then(function (e) {
            return ++n._retryCount, e
        })
    }, Xn.prototype.clone = function (e) {
        return I.defined(e) || (e = new Xn({url: this._url})), e._url = this._url, e._formParameters = ln(this._formParameters), e._templateValues = ln(this._templateValues), e.headers = ln(this.headers), e.proxy = this.proxy, e.retryCallback = this.retryCallback, e.retryAttempts = this.retryAttempts, e._retryCount = 0, e.request = this.request.clone(), e
    }, Xn.prototype.getBaseUri = function (e) {
        return t = this.getUrlComponent(e), n = e, r = "", -1 !== (a = t.lastIndexOf("/")) && (r = t.substring(0, a + 1)), n && (t = new tn(t), I.defined(t.query) && (r += "?" + t.query), I.defined(t.fragment) && (r += "#" + t.fragment)), r;
        var t, n, r, a
    }, Xn.prototype.appendForwardSlash = function () {
        var e;
        this._url = (0 !== (e = this._url).length && "/" === e[e.length - 1] || (e += "/"), e)
    }, Xn.prototype.fetchArrayBuffer = function () {
        return this.fetch({responseType: "arraybuffer"})
    }, Xn.fetchArrayBuffer = function (e) {
        return new Xn(e).fetchArrayBuffer()
    }, Xn.prototype.fetchBlob = function () {
        return this.fetch({responseType: "blob"})
    }, Xn.fetchBlob = function (e) {
        return new Xn(e).fetchBlob()
    }, Xn.prototype.fetchImage = function (e) {
        e = I.defaultValue(e, I.defaultValue.EMPTY_OBJECT);
        var t = I.defaultValue(e.preferImageBitmap, !1), n = I.defaultValue(e.preferBlob, !1),
            r = I.defaultValue(e.flipY, !1);
        if (Yn(this.request), !Bn || this.isDataUri || this.isBlobUri || !this.hasHeaders && !n) return Hn({
            resource: this,
            flipY: r,
            preferImageBitmap: t
        });
        var a, i, s, o = this.fetchBlob();
        return I.defined(o) ? Xn.supportsImageBitmapOptions().then(function (e) {
            return a = e && t, o
        }).then(function (e) {
            if (I.defined(e)) {
                if (s = e, a) return Xn.createImageBitmapFromBlob(e, {flipY: r, premultiplyAlpha: !1});
                var t = window.URL.createObjectURL(e);
                return Hn({resource: i = new Xn({url: t}), flipY: r, preferImageBitmap: !1})
            }
        }).then(function (e) {
            if (I.defined(e)) return e.blob = s, a || window.URL.revokeObjectURL(i.url), e
        }).otherwise(function (e) {
            return I.defined(i) && window.URL.revokeObjectURL(i.url), e.blob = s, I.when.reject(e)
        }) : void 0
    }, Xn.fetchImage = function (e) {
        return new Xn(e).fetchImage({flipY: e.flipY, preferBlob: e.preferBlob, preferImageBitmap: e.preferImageBitmap})
    }, Xn.prototype.fetchText = function () {
        return this.fetch({responseType: "text"})
    }, Xn.fetchText = function (e) {
        return new Xn(e).fetchText()
    }, Xn.prototype.fetchJson = function () {
        var e = this.fetch({responseType: "text", headers: {Accept: "application/json,*/*;q=0.01"}});
        if (I.defined(e)) return e.then(function (e) {
            if (I.defined(e)) return JSON.parse(e)
        })
    }, Xn.fetchJson = function (e) {
        return new Xn(e).fetchJson()
    }, Xn.prototype.fetchXML = function () {
        return this.fetch({responseType: "document", overrideMimeType: "text/xml"})
    }, Xn.fetchXML = function (e) {
        return new Xn(e).fetchXML()
    }, Xn.prototype.fetchJsonp = function (e) {
        var t;
        for (e = I.defaultValue(e, "callback"), Yn(this.request); t = "loadJsonp" + Math.random().toString().substring(2, 8), I.defined(window[t]);) ;
        return function n(r, a, i) {
            var e = {};
            e[a] = i;
            r.setQueryParameters(e);
            var s = r.request;
            s.url = r.url;
            s.requestFunction = function () {
                var t = I.when.defer();
                return window[i] = function (e) {
                    t.resolve(e);
                    try {
                        delete window[i]
                    } catch (e) {
                        window[i] = void 0
                    }
                }, Xn._Implementations.loadAndExecuteScript(r.url, i, t), t.promise
            };
            var t = zn.request(s);
            if (!I.defined(t)) return;
            return t.otherwise(function (t) {
                return s.state !== vn.FAILED ? I.when.reject(t) : r.retryOnError(t).then(function (e) {
                    return e ? (s.state = vn.UNISSUED, s.deferred = void 0, n(r, a, i)) : I.when.reject(t)
                })
            })
        }(this, e, t)
    }, Xn.fetchJsonp = function (e) {
        return new Xn(e).fetchJsonp(e.callbackParameterName)
    }, Xn.prototype._makeRequest = function (o) {
        var u = this;
        Yn(u.request);
        var l = u.request;
        l.url = u.url, l.requestFunction = function () {
            var e = o.responseType, t = cn(o.headers, u.headers), n = o.overrideMimeType, r = o.method, a = o.data,
                i = I.when.defer(), s = Xn._Implementations.loadWithXhr(u.url, e, r, a, t, i, n);
            return I.defined(s) && I.defined(s.abort) && (l.cancelFunction = function () {
                s.abort()
            }), i.promise
        };
        var e = zn.request(l);
        if (I.defined(e)) return e.then(function (e) {
            return e
        }).otherwise(function (t) {
            return l.state !== vn.FAILED ? I.when.reject(t) : u.retryOnError(t).then(function (e) {
                return e ? (l.state = vn.UNISSUED, l.deferred = void 0, u.fetch(o)) : I.when.reject(t)
            })
        })
    };
    var Jn = /^data:(.*?)(;base64)?,(.*)$/;

    function Gn(e, t) {
        var n = decodeURIComponent(t);
        return e ? atob(n) : n
    }

    function Qn(e, t) {
        for (var n = Gn(e, t), r = new ArrayBuffer(n.length), a = new Uint8Array(r), i = 0; i < n.length; i++) a[i] = n.charCodeAt(i);
        return r
    }

    function Kn(e, t) {
        switch (t) {
            case"text":
                return e.toString("utf8");
            case"json":
                return JSON.parse(e.toString("utf8"));
            default:
                return new Uint8Array(e).buffer
        }
    }

    Xn.prototype.fetch = function (e) {
        return (e = kn(e, {})).method = "GET", this._makeRequest(e)
    }, Xn.fetch = function (e) {
        return new Xn(e).fetch({responseType: e.responseType, overrideMimeType: e.overrideMimeType})
    }, Xn.prototype.delete = function (e) {
        return (e = kn(e, {})).method = "DELETE", this._makeRequest(e)
    }, Xn.delete = function (e) {
        return new Xn(e).delete({responseType: e.responseType, overrideMimeType: e.overrideMimeType, data: e.data})
    }, Xn.prototype.head = function (e) {
        return (e = kn(e, {})).method = "HEAD", this._makeRequest(e)
    }, Xn.head = function (e) {
        return new Xn(e).head({responseType: e.responseType, overrideMimeType: e.overrideMimeType})
    }, Xn.prototype.options = function (e) {
        return (e = kn(e, {})).method = "OPTIONS", this._makeRequest(e)
    }, Xn.options = function (e) {
        return new Xn(e).options({responseType: e.responseType, overrideMimeType: e.overrideMimeType})
    }, Xn.prototype.post = function (e, t) {
        return i.Check.defined("data", e), (t = kn(t, {})).method = "POST", t.data = e, this._makeRequest(t)
    }, Xn.post = function (e) {
        return new Xn(e).post(e.data, {responseType: e.responseType, overrideMimeType: e.overrideMimeType})
    }, Xn.prototype.put = function (e, t) {
        return i.Check.defined("data", e), (t = kn(t, {})).method = "PUT", t.data = e, this._makeRequest(t)
    }, Xn.put = function (e) {
        return new Xn(e).put(e.data, {responseType: e.responseType, overrideMimeType: e.overrideMimeType})
    }, Xn.prototype.patch = function (e, t) {
        return i.Check.defined("data", e), (t = kn(t, {})).method = "PATCH", t.data = e, this._makeRequest(t)
    }, Xn.patch = function (e) {
        return new Xn(e).patch(e.data, {responseType: e.responseType, overrideMimeType: e.overrideMimeType})
    }, (Xn._Implementations = {}).createImage = function (o, u, l, c, d) {
        var f = o.url;
        Xn.supportsImageBitmapOptions().then(function (e) {
            if (!e || !d) return t = f, n = u, r = l, (a = new Image).onload = function () {
                r.resolve(a)
            }, a.onerror = function (e) {
                r.reject(e)
            }, n && (Fn.contains(t) ? a.crossOrigin = "use-credentials" : a.crossOrigin = ""), void (a.src = t);
            var t, n, r, a, i = I.when.defer(),
                s = Xn._Implementations.loadWithXhr(f, "blob", "GET", void 0, void 0, i, void 0, void 0, void 0);
            return I.defined(s) && I.defined(s.abort) && (o.cancelFunction = function () {
                s.abort()
            }), i.promise.then(function (e) {
                if (I.defined(e)) return Xn.createImageBitmapFromBlob(e, {flipY: c, premultiplyAlpha: !1});
                l.reject(new H.RuntimeError("Successfully retrieved " + f + " but it contained no content."))
            }).then(l.resolve)
        }).otherwise(l.reject)
    }, Xn.createImageBitmapFromBlob = function (e, t) {
        return i.Check.defined("options", t), i.Check.typeOf.bool("options.flipY", t.flipY), i.Check.typeOf.bool("options.premultiplyAlpha", t.premultiplyAlpha), createImageBitmap(e, {
            imageOrientation: t.flipY ? "flipY" : "none",
            premultiplyAlpha: t.premultiplyAlpha ? "premultiply" : "none"
        })
    };
    var $n = "undefined" == typeof XMLHttpRequest;

    function er(e) {
        if (e = I.defaultValue(e, I.defaultValue.EMPTY_OBJECT), this._dates = void 0, this._samples = void 0, this._dateColumn = -1, this._xPoleWanderRadiansColumn = -1, this._yPoleWanderRadiansColumn = -1, this._ut1MinusUtcSecondsColumn = -1, this._xCelestialPoleOffsetRadiansColumn = -1, this._yCelestialPoleOffsetRadiansColumn = -1, this._taiMinusUtcSecondsColumn = -1, this._columnCount = 0, this._lastIndex = -1, this._downloadPromise = void 0, this._dataError = void 0, this._addNewLeapSeconds = I.defaultValue(e.addNewLeapSeconds, !0), I.defined(e.data)) nr(this, e.data); else if (I.defined(e.url)) {
            var t = Xn.createIfNeeded(e.url), n = this;
            this._downloadPromise = I.when(t.fetchJson(), function (e) {
                nr(n, e)
            }, function () {
                n._dataError = "An error occurred while retrieving the EOP data from the URL " + t.url + "."
            })
        } else nr(this, {
            columnNames: ["dateIso8601", "modifiedJulianDateUtc", "xPoleWanderRadians", "yPoleWanderRadians", "ut1MinusUtcSeconds", "lengthOfDayCorrectionSeconds", "xCelestialPoleOffsetRadians", "yCelestialPoleOffsetRadians", "taiMinusUtcSeconds"],
            samples: []
        })
    }

    function tr(e, t) {
        return $t.compare(e.julianDate, t)
    }

    function nr(e, t) {
        if (I.defined(t.columnNames)) if (I.defined(t.samples)) {
            var n = t.columnNames.indexOf("modifiedJulianDateUtc"), r = t.columnNames.indexOf("xPoleWanderRadians"),
                a = t.columnNames.indexOf("yPoleWanderRadians"), i = t.columnNames.indexOf("ut1MinusUtcSeconds"),
                s = t.columnNames.indexOf("xCelestialPoleOffsetRadians"),
                o = t.columnNames.indexOf("yCelestialPoleOffsetRadians"),
                u = t.columnNames.indexOf("taiMinusUtcSeconds");
            if (n < 0 || r < 0 || a < 0 || i < 0 || s < 0 || o < 0 || u < 0) e._dataError = "Error in loaded EOP data: The columnNames property must include modifiedJulianDateUtc, xPoleWanderRadians, yPoleWanderRadians, ut1MinusUtcSeconds, xCelestialPoleOffsetRadians, yCelestialPoleOffsetRadians, and taiMinusUtcSeconds columns"; else {
                var l, c = e._samples = t.samples, d = e._dates = [];
                e._dateColumn = n, e._xPoleWanderRadiansColumn = r, e._yPoleWanderRadiansColumn = a, e._ut1MinusUtcSecondsColumn = i, e._xCelestialPoleOffsetRadiansColumn = s, e._yCelestialPoleOffsetRadiansColumn = o, e._taiMinusUtcSecondsColumn = u, e._columnCount = t.columnNames.length, e._lastIndex = void 0;
                for (var f = e._addNewLeapSeconds, h = 0, p = c.length; h < p; h += e._columnCount) {
                    var m = c[h + n], y = c[h + u], v = new $t(m + Pt.MODIFIED_JULIAN_DATE_DIFFERENCE, y, Dt.TAI);
                    if (d.push(v), f) {
                        if (y !== l && I.defined(l)) {
                            var C = $t.leapSeconds, w = Rt(C, v, tr);
                            if (w < 0) {
                                var g = new It(v, y);
                                C.splice(~w, 0, g)
                            }
                        }
                        l = y
                    }
                }
            }
        } else e._dataError = "Error in loaded EOP data: The samples property is required."; else e._dataError = "Error in loaded EOP data: The columnNames property is required."
    }

    function rr(e, t, n, r, a) {
        var i = n * r;
        a.xPoleWander = t[i + e._xPoleWanderRadiansColumn], a.yPoleWander = t[i + e._yPoleWanderRadiansColumn], a.xPoleOffset = t[i + e._xCelestialPoleOffsetRadiansColumn], a.yPoleOffset = t[i + e._yCelestialPoleOffsetRadiansColumn], a.ut1MinusUtc = t[i + e._ut1MinusUtcSecondsColumn]
    }

    function ar(e, t, n) {
        return t + e * (n - t)
    }

    function ir(e, t, n, r, a, i, s) {
        var o = e._columnCount;
        if (i > t.length - 1) return s.xPoleWander = 0, s.yPoleWander = 0, s.xPoleOffset = 0, s.yPoleOffset = 0, s.ut1MinusUtc = 0, s;
        var u = t[a], l = t[i];
        if (u.equals(l) || r.equals(u)) return rr(e, n, a, o, s), s;
        if (r.equals(l)) return rr(e, n, i, o, s), s;
        var c = $t.secondsDifference(r, u) / $t.secondsDifference(l, u), d = a * o, f = i * o,
            h = n[d + e._ut1MinusUtcSecondsColumn], p = n[f + e._ut1MinusUtcSecondsColumn], m = p - h;
        if (.5 < m || m < -.5) {
            var y = n[d + e._taiMinusUtcSecondsColumn], v = n[f + e._taiMinusUtcSecondsColumn];
            y !== v && (l.equals(r) ? h = p : p -= v - y)
        }
        return s.xPoleWander = ar(c, n[d + e._xPoleWanderRadiansColumn], n[f + e._xPoleWanderRadiansColumn]), s.yPoleWander = ar(c, n[d + e._yPoleWanderRadiansColumn], n[f + e._yPoleWanderRadiansColumn]), s.xPoleOffset = ar(c, n[d + e._xCelestialPoleOffsetRadiansColumn], n[f + e._xCelestialPoleOffsetRadiansColumn]), s.yPoleOffset = ar(c, n[d + e._yCelestialPoleOffsetRadiansColumn], n[f + e._yCelestialPoleOffsetRadiansColumn]), s.ut1MinusUtc = ar(c, h, p), s
    }

    function sr(e, t, n) {
        this.heading = I.defaultValue(e, 0), this.pitch = I.defaultValue(t, 0), this.roll = I.defaultValue(n, 0)
    }

    Xn._Implementations.loadWithXhr = function (e, a, i, t, n, s, r) {
        var o = Jn.exec(e);
        if (null === o) {
            if ($n) return u = e, l = a, c = i, d = n, f = s, h = require("url").parse(u), p = "https:" === h.protocol ? require("https") : require("http"), m = require("zlib"), y = {
                protocol: h.protocol,
                hostname: h.hostname,
                port: h.port,
                path: h.path,
                query: h.query,
                method: c,
                headers: d
            }, void p.request(y).on("response", function (t) {
                if (t.statusCode < 200 || 300 <= t.statusCode) f.reject(new gn(t.statusCode, t, t.headers)); else {
                    var n = [];
                    t.on("data", function (e) {
                        n.push(e)
                    }), t.on("end", function () {
                        var e = Buffer.concat(n);
                        "gzip" === t.headers["content-encoding"] ? m.gunzip(e, function (e, t) {
                            e ? f.reject(new H.RuntimeError("Error decompressing response.")) : f.resolve(Kn(t, l))
                        }) : f.resolve(Kn(e, l))
                    })
                }
            }).on("error", function (e) {
                f.reject(new gn)
            }).end();
            var u, l, c, d, f, h, p, m, y, v = new XMLHttpRequest;
            if (Fn.contains(e) && (v.withCredentials = !0), v.open(i, e, !0), I.defined(r) && I.defined(v.overrideMimeType) && v.overrideMimeType(r), I.defined(n)) for (var C in n) n.hasOwnProperty(C) && v.setRequestHeader(C, n[C]);
            I.defined(a) && (v.responseType = a);
            var w = !1;
            return "string" == typeof e && (w = 0 === e.indexOf("file://") || "undefined" != typeof window && "file://" === window.location.origin), v.onload = function () {
                if (!(v.status < 200 || 300 <= v.status) || w && 0 === v.status) {
                    var e = v.response, t = v.responseType;
                    if ("HEAD" === i || "OPTIONS" === i) {
                        var n = v.getAllResponseHeaders().trim().split(/[\r\n]+/), r = {};
                        return n.forEach(function (e) {
                            var t = e.split(": "), n = t.shift();
                            r[n] = t.join(": ")
                        }), void s.resolve(r)
                    }
                    if (204 === v.status) s.resolve(); else if (!I.defined(e) || I.defined(a) && t !== a) if ("json" === a && "string" == typeof e) try {
                        s.resolve(JSON.parse(e))
                    } catch (e) {
                        s.reject(e)
                    } else ("" === t || "document" === t) && I.defined(v.responseXML) && v.responseXML.hasChildNodes() ? s.resolve(v.responseXML) : "" !== t && "text" !== t || !I.defined(v.responseText) ? s.reject(new H.RuntimeError("Invalid XMLHttpRequest response type.")) : s.resolve(v.responseText); else s.resolve(e)
                } else s.reject(new gn(v.status, v.response, v.getAllResponseHeaders()))
            }, v.onerror = function (e) {
                s.reject(new gn)
            }, v.send(t), v
        }
        s.resolve(function (e, t) {
            t = I.defaultValue(t, "");
            var n = e[1], r = !!e[2], a = e[3];
            switch (t) {
                case"":
                case"text":
                    return Gn(r, a);
                case"arraybuffer":
                    return Qn(r, a);
                case"blob":
                    var i = Qn(r, a);
                    return new Blob([i], {type: n});
                case"document":
                    return (new DOMParser).parseFromString(Gn(r, a), n);
                case"json":
                    return JSON.parse(Gn(r, a))
            }
        }(o, a))
    }, Xn._Implementations.loadAndExecuteScript = function (e, t, n) {
        return function (e) {
            var t = I.when.defer(), n = document.createElement("script");
            n.async = !0, n.src = e;
            var r = document.getElementsByTagName("head")[0];
            return n.onload = function () {
                n.onload = void 0, r.removeChild(n), t.resolve()
            }, n.onerror = function (e) {
                t.reject(e)
            }, r.appendChild(n), t.promise
        }(e).otherwise(n.reject)
    }, (Xn._DefaultImplementations = {}).createImage = Xn._Implementations.createImage, Xn._DefaultImplementations.loadWithXhr = Xn._Implementations.loadWithXhr, Xn._DefaultImplementations.loadAndExecuteScript = Xn._Implementations.loadAndExecuteScript, Xn.DEFAULT = Object.freeze(new Xn({url: "undefined" == typeof document ? "" : document.location.href.split("?")[0]})), er.NONE = Object.freeze({
        getPromiseToLoad: function () {
            return I.when()
        }, compute: function (e, t) {
            return I.defined(t) ? (t.xPoleWander = 0, t.yPoleWander = 0, t.xPoleOffset = 0, t.yPoleOffset = 0, t.ut1MinusUtc = 0) : t = new At(0, 0, 0, 0, 0), t
        }
    }), er.prototype.getPromiseToLoad = function () {
        return I.when(this._downloadPromise)
    }, er.prototype.compute = function (e, t) {
        if (I.defined(this._samples)) {
            if (I.defined(t) || (t = new At(0, 0, 0, 0, 0)), 0 === this._samples.length) return t.xPoleWander = 0, t.yPoleWander = 0, t.xPoleOffset = 0, t.yPoleOffset = 0, t.ut1MinusUtc = 0, t;
            var n = this._dates, r = this._lastIndex, a = 0, i = 0;
            if (I.defined(r)) {
                var s = n[r], o = n[r + 1], u = $t.lessThanOrEquals(s, e), l = !I.defined(o),
                    c = l || $t.greaterThanOrEquals(o, e);
                if (u && c) return a = r, !l && o.equals(e) && ++a, i = a + 1, ir(this, n, this._samples, e, a, i, t), t
            }
            var d = Rt(n, e, $t.compare, this._dateColumn);
            return 0 <= d ? (d < n.length - 1 && n[d + 1].equals(e) && ++d, i = a = d) : (a = (i = ~d) - 1) < 0 && (a = 0), this._lastIndex = a, ir(this, n, this._samples, e, a, i, t), t
        }
        if (I.defined(this._dataError)) throw new H.RuntimeError(this._dataError)
    }, sr.fromQuaternion = function (e, t) {
        I.defined(t) || (t = new sr);
        var n = 2 * (e.w * e.y - e.z * e.x), r = 1 - 2 * (e.x * e.x + e.y * e.y), a = 2 * (e.w * e.x + e.y * e.z),
            i = 1 - 2 * (e.y * e.y + e.z * e.z), s = 2 * (e.w * e.z + e.x * e.y);
        return t.heading = -Math.atan2(s, i), t.roll = Math.atan2(a, r), t.pitch = -X.CesiumMath.asinClamped(n), t
    }, sr.fromDegrees = function (e, t, n, r) {
        return I.defined(r) || (r = new sr), r.heading = e * X.CesiumMath.RADIANS_PER_DEGREE, r.pitch = t * X.CesiumMath.RADIANS_PER_DEGREE, r.roll = n * X.CesiumMath.RADIANS_PER_DEGREE, r
    }, sr.clone = function (e, t) {
        if (I.defined(e)) return I.defined(t) ? (t.heading = e.heading, t.pitch = e.pitch, t.roll = e.roll, t) : new sr(e.heading, e.pitch, e.roll)
    }, sr.equals = function (e, t) {
        return e === t || I.defined(e) && I.defined(t) && e.heading === t.heading && e.pitch === t.pitch && e.roll === t.roll
    }, sr.equalsEpsilon = function (e, t, n, r) {
        return e === t || I.defined(e) && I.defined(t) && X.CesiumMath.equalsEpsilon(e.heading, t.heading, n, r) && X.CesiumMath.equalsEpsilon(e.pitch, t.pitch, n, r) && X.CesiumMath.equalsEpsilon(e.roll, t.roll, n, r)
    }, sr.prototype.clone = function (e) {
        return sr.clone(this, e)
    }, sr.prototype.equals = function (e) {
        return sr.equals(this, e)
    }, sr.prototype.equalsEpsilon = function (e, t, n) {
        return sr.equalsEpsilon(this, e, t, n)
    }, sr.prototype.toString = function () {
        return "(" + this.heading + ", " + this.pitch + ", " + this.roll + ")"
    };
    var or, ur, lr, cr = /((?:.*\/)|^)Cesium\.js$/;

    function dr(e) {
        return "undefined" == typeof document ? e : (I.defined(or) || (or = document.createElement("a")), or.href = e, or.href = or.href, or.href)
    }

    function fr() {
        return I.defined(ur) || (e = "undefined" != typeof CESIUM_BASE_URL ? CESIUM_BASE_URL : "object" == typeof define && I.defined(define.amd) && !define.amd.toUrlUndefined && I.defined(require.toUrl) ? dn("..", mr("Core/buildModuleUrl.js")) : function () {
            for (var e = document.getElementsByTagName("script"), t = 0, n = e.length; t < n; ++t) {
                var r = e[t].getAttribute("src"), a = cr.exec(r);
                if (null !== a) return a[1]
            }
        }(), (ur = new Xn({url: dr(e)})).appendForwardSlash()), ur;
        var e
    }

    function hr(e) {
        return dr(require.toUrl("../" + e))
    }

    function pr(e) {
        return fr().getDerivedResource({url: e}).url
    }

    function mr(e) {
        return I.defined(lr) || (lr = "object" == typeof define && I.defined(define.amd) && !define.amd.toUrlUndefined && I.defined(require.toUrl) ? hr : pr), lr(e)
    }

    function yr(e, t, n) {
        this.x = e, this.y = t, this.s = n
    }

    function vr(e) {
        e = I.defaultValue(e, I.defaultValue.EMPTY_OBJECT), this._xysFileUrlTemplate = Xn.createIfNeeded(e.xysFileUrlTemplate), this._interpolationOrder = I.defaultValue(e.interpolationOrder, 9), this._sampleZeroJulianEphemerisDate = I.defaultValue(e.sampleZeroJulianEphemerisDate, 2442396.5), this._sampleZeroDateTT = new $t(this._sampleZeroJulianEphemerisDate, 0, Dt.TAI), this._stepSizeDays = I.defaultValue(e.stepSizeDays, 1), this._samplesPerXysFile = I.defaultValue(e.samplesPerXysFile, 1e3), this._totalSamples = I.defaultValue(e.totalSamples, 27426), this._samples = new Array(3 * this._totalSamples), this._chunkDownloadsInProgress = [];
        for (var t = this._interpolationOrder, n = this._denominators = new Array(t + 1), r = this._xTable = new Array(t + 1), a = Math.pow(this._stepSizeDays, t), i = 0; i <= t; ++i) {
            n[i] = a, r[i] = i * this._stepSizeDays;
            for (var s = 0; s <= t; ++s) s !== i && (n[i] *= i - s);
            n[i] = 1 / n[i]
        }
        this._work = new Array(t + 1), this._coef = new Array(t + 1)
    }

    mr._cesiumScriptRegex = cr, mr._buildModuleUrlFromBaseUrl = pr, mr._clearBaseResource = function () {
        ur = void 0
    }, mr.setBaseUrl = function (e) {
        ur = Xn.DEFAULT.getDerivedResource({url: e})
    }, mr.getCesiumBaseUrl = fr;
    var Cr = new $t(0, 0, Dt.TAI);

    function wr(e, t, n) {
        var r = Cr;
        return r.dayNumber = t, r.secondsOfDay = n, $t.daysDifference(r, e._sampleZeroDateTT)
    }

    function gr(s, o) {
        if (s._chunkDownloadsInProgress[o]) return s._chunkDownloadsInProgress[o];
        var e, u = I.when.defer();
        s._chunkDownloadsInProgress[o] = u;
        var t = s._xysFileUrlTemplate;
        return e = I.defined(t) ? t.getDerivedResource({templateValues: {0: o}}) : new Xn({url: mr("Assets/IAU2006_XYS/IAU2006_XYS_" + o + ".json")}), I.when(e.fetchJson(), function (e) {
            s._chunkDownloadsInProgress[o] = !1;
            for (var t = s._samples, n = e.samples, r = o * s._samplesPerXysFile * 3, a = 0, i = n.length; a < i; ++a) t[r + a] = n[a];
            u.resolve()
        }), u.promise
    }

    vr.prototype.preload = function (e, t, n, r) {
        var a = wr(this, e, t), i = wr(this, n, r), s = a / this._stepSizeDays - this._interpolationOrder / 2 | 0;
        s < 0 && (s = 0);
        var o = i / this._stepSizeDays - this._interpolationOrder / 2 | 0 + this._interpolationOrder;
        o >= this._totalSamples && (o = this._totalSamples - 1);
        for (var u = s / this._samplesPerXysFile | 0, l = o / this._samplesPerXysFile | 0, c = [], d = u; d <= l; ++d) c.push(gr(this, d));
        return I.when.all(c)
    }, vr.prototype.computeXysRadians = function (e, t, n) {
        var r = wr(this, e, t);
        if (!(r < 0)) {
            var a = r / this._stepSizeDays | 0;
            if (!(a >= this._totalSamples)) {
                var i = this._interpolationOrder, s = a - (i / 2 | 0);
                s < 0 && (s = 0);
                var o = s + i;
                o >= this._totalSamples && (s = (o = this._totalSamples - 1) - i) < 0 && (s = 0);
                var u = !1, l = this._samples;
                if (I.defined(l[3 * s]) || (gr(this, s / this._samplesPerXysFile | 0), u = !0), I.defined(l[3 * o]) || (gr(this, o / this._samplesPerXysFile | 0), u = !0), !u) {
                    I.defined(n) ? (n.x = 0, n.y = 0, n.s = 0) : n = new yr(0, 0, 0);
                    var c, d, f = r - s * this._stepSizeDays, h = this._work, p = this._denominators, m = this._coef,
                        y = this._xTable;
                    for (c = 0; c <= i; ++c) h[c] = f - y[c];
                    for (c = 0; c <= i; ++c) {
                        for (m[c] = 1, d = 0; d <= i; ++d) d !== c && (m[c] *= h[d]);
                        m[c] *= p[c];
                        var v = 3 * (s + c);
                        n.x += m[c] * l[v++], n.y += m[c] * l[v++], n.s += m[c] * l[v]
                    }
                    return n
                }
            }
        }
    };
    var xr = {}, Er = {
            up: {south: "east", north: "west", west: "south", east: "north"},
            down: {south: "west", north: "east", west: "north", east: "south"},
            south: {up: "west", down: "east", west: "down", east: "up"},
            north: {up: "east", down: "west", west: "up", east: "down"},
            west: {up: "north", down: "south", north: "down", south: "up"},
            east: {up: "south", down: "north", north: "up", south: "down"}
        }, Or = {north: [-1, 0, 0], east: [0, 1, 0], up: [0, 0, 1], south: [1, 0, 0], west: [0, -1, 0], down: [0, 0, -1]},
        _r = {}, Sr = {
            east: new P.Cartesian3,
            north: new P.Cartesian3,
            up: new P.Cartesian3,
            west: new P.Cartesian3,
            south: new P.Cartesian3,
            down: new P.Cartesian3
        }, br = new P.Cartesian3, Mr = new P.Cartesian3, Rr = new P.Cartesian3;
    xr.localFrameToFixedFrameGenerator = function (s, o) {
        if (!Er.hasOwnProperty(s) || !Er[s].hasOwnProperty(o)) throw new i.DeveloperError("firstAxis and secondAxis must be east, north, up, west, south or down.");
        var e, u = Er[s][o], t = s + o;
        return I.defined(_r[t]) ? e = _r[t] : (e = function (e, t, n) {
            if (I.defined(n) || (n = new Q), P.Cartesian3.equalsEpsilon(e, P.Cartesian3.ZERO, X.CesiumMath.EPSILON14)) P.Cartesian3.unpack(Or[s], 0, br), P.Cartesian3.unpack(Or[o], 0, Mr), P.Cartesian3.unpack(Or[u], 0, Rr); else if (X.CesiumMath.equalsEpsilon(e.x, 0, X.CesiumMath.EPSILON14) && X.CesiumMath.equalsEpsilon(e.y, 0, X.CesiumMath.EPSILON14)) {
                var r = X.CesiumMath.sign(e.z);
                P.Cartesian3.unpack(Or[s], 0, br), "east" !== s && "west" !== s && P.Cartesian3.multiplyByScalar(br, r, br), P.Cartesian3.unpack(Or[o], 0, Mr), "east" !== o && "west" !== o && P.Cartesian3.multiplyByScalar(Mr, r, Mr), P.Cartesian3.unpack(Or[u], 0, Rr), "east" !== u && "west" !== u && P.Cartesian3.multiplyByScalar(Rr, r, Rr)
            } else {
                (t = I.defaultValue(t, P.Ellipsoid.WGS84)).geodeticSurfaceNormal(e, Sr.up);
                var a = Sr.up, i = Sr.east;
                i.x = -e.y, i.y = e.x, i.z = 0, P.Cartesian3.normalize(i, Sr.east), P.Cartesian3.cross(a, i, Sr.north), P.Cartesian3.multiplyByScalar(Sr.up, -1, Sr.down), P.Cartesian3.multiplyByScalar(Sr.east, -1, Sr.west), P.Cartesian3.multiplyByScalar(Sr.north, -1, Sr.south), br = Sr[s], Mr = Sr[o], Rr = Sr[u]
            }
            return n[0] = br.x, n[1] = br.y, n[2] = br.z, n[3] = 0, n[4] = Mr.x, n[5] = Mr.y, n[6] = Mr.z, n[7] = 0, n[8] = Rr.x, n[9] = Rr.y, n[10] = Rr.z, n[11] = 0, n[12] = e.x, n[13] = e.y, n[14] = e.z, n[15] = 1, n
        }, _r[t] = e), e
    }, xr.eastNorthUpToFixedFrame = xr.localFrameToFixedFrameGenerator("east", "north"), xr.northEastDownToFixedFrame = xr.localFrameToFixedFrameGenerator("north", "east"), xr.northUpEastToFixedFrame = xr.localFrameToFixedFrameGenerator("north", "up"), xr.northWestUpToFixedFrame = xr.localFrameToFixedFrameGenerator("north", "west");
    var Ar = new Ke, Tr = new P.Cartesian3(1, 1, 1), qr = new Q;
    xr.headingPitchRollToFixedFrame = function (e, t, n, r, a) {
        r = I.defaultValue(r, xr.eastNorthUpToFixedFrame);
        var i = Ke.fromHeadingPitchRoll(t, Ar),
            s = Q.fromTranslationQuaternionRotationScale(P.Cartesian3.ZERO, i, Tr, qr);
        return a = r(e, n, a), Q.multiply(a, s, a)
    };
    var zr = new Q, Ir = new J;
    xr.headingPitchRollQuaternion = function (e, t, n, r, a) {
        var i = xr.headingPitchRollToFixedFrame(e, t, n, r, zr), s = Q.getMatrix3(i, Ir);
        return Ke.fromRotationMatrix(s, a)
    };
    var Pr = new P.Cartesian3(1, 1, 1), Dr = new P.Cartesian3, Nr = new Q, Ur = new Q, Fr = new J, Vr = new Ke;
    xr.fixedFrameToHeadingPitchRoll = function (e, t, n, r) {
        t = I.defaultValue(t, P.Ellipsoid.WGS84), n = I.defaultValue(n, xr.eastNorthUpToFixedFrame), I.defined(r) || (r = new sr);
        var a = Q.getTranslation(e, Dr);
        if (P.Cartesian3.equals(a, P.Cartesian3.ZERO)) return r.heading = 0, r.pitch = 0, r.roll = 0, r;
        var i = Q.inverseTransformation(n(a, t, Nr), Nr), s = Q.setScale(e, Pr, Ur);
        s = Q.setTranslation(s, P.Cartesian3.ZERO, s), i = Q.multiply(i, s, i);
        var o = Ke.fromRotationMatrix(Q.getMatrix3(i, Fr), Vr);
        return o = Ke.normalize(o, o), sr.fromQuaternion(o, r)
    };
    var Lr = X.CesiumMath.TWO_PI / 86400, Br = new $t;
    xr.computeTemeToPseudoFixedMatrix = function (e, t) {
        var n, r = (Br = $t.addSeconds(e, -$t.computeTaiMinusUtc(e), Br)).dayNumber, a = Br.secondsOfDay,
            i = r - 2451545,
            s = (24110.54841 + (n = 43200 <= a ? (.5 + i) / Pt.DAYS_PER_JULIAN_CENTURY : (i - .5) / Pt.DAYS_PER_JULIAN_CENTURY) * (8640184.812866 + n * (.093104 + -62e-7 * n))) * Lr % X.CesiumMath.TWO_PI + (72921158553e-15 + 11772758384668e-32 * (r - 2451545.5)) * ((a + .5 * Pt.SECONDS_PER_DAY) % Pt.SECONDS_PER_DAY),
            o = Math.cos(s), u = Math.sin(s);
        return I.defined(t) ? (t[0] = o, t[1] = -u, t[2] = 0, t[3] = u, t[4] = o, t[5] = 0, t[6] = 0, t[7] = 0, t[8] = 1, t) : new J(o, u, 0, -u, o, 0, 0, 0, 1)
    }, xr.iau2006XysData = new vr, xr.earthOrientationParameters = er.NONE;
    var Wr = 32.184;
    xr.preloadIcrfFixed = function (e) {
        var t = e.start.dayNumber, n = e.start.secondsOfDay + Wr, r = e.stop.dayNumber, a = e.stop.secondsOfDay + Wr,
            i = xr.iau2006XysData.preload(t, n, r, a), s = xr.earthOrientationParameters.getPromiseToLoad();
        return I.when.all([i, s])
    }, xr.computeIcrfToFixedMatrix = function (e, t) {
        I.defined(t) || (t = new J);
        var n = xr.computeFixedToIcrfMatrix(e, t);
        if (I.defined(n)) return J.transpose(n, t)
    };
    var jr = new yr(0, 0, 0), kr = new At(0, 0, 0, 0, 0), Yr = new J, Zr = new J;
    xr.computeFixedToIcrfMatrix = function (e, t) {
        I.defined(t) || (t = new J);
        var n = xr.earthOrientationParameters.compute(e, kr);
        if (I.defined(n)) {
            var r = e.dayNumber, a = e.secondsOfDay + Wr, i = xr.iau2006XysData.computeXysRadians(r, a, jr);
            if (I.defined(i)) {
                var s = i.x + n.xPoleOffset, o = i.y + n.yPoleOffset, u = 1 / (1 + Math.sqrt(1 - s * s - o * o)),
                    l = Yr;
                l[0] = 1 - u * s * s, l[3] = -u * s * o, l[6] = s, l[1] = -u * s * o, l[4] = 1 - u * o * o, l[7] = o, l[2] = -s, l[5] = -o, l[8] = 1 - u * (s * s + o * o);
                var c = J.fromRotationZ(-i.s, Zr), d = J.multiply(l, c, Yr), f = e.dayNumber - 2451545,
                    h = (e.secondsOfDay - $t.computeTaiMinusUtc(e) + n.ut1MinusUtc) / Pt.SECONDS_PER_DAY,
                    p = .779057273264 + h + .00273781191135448 * (f + h);
                p = p % 1 * X.CesiumMath.TWO_PI;
                var m = J.fromRotationZ(p, Zr), y = J.multiply(d, m, Yr), v = Math.cos(n.xPoleWander),
                    C = Math.cos(n.yPoleWander), w = Math.sin(n.xPoleWander), g = Math.sin(n.yPoleWander),
                    x = r - 2451545 + a / Pt.SECONDS_PER_DAY,
                    E = -47e-6 * (x /= 36525) * X.CesiumMath.RADIANS_PER_DEGREE / 3600, O = Math.cos(E),
                    _ = Math.sin(E), S = Zr;
                return S[0] = v * O, S[1] = v * _, S[2] = w, S[3] = -C * _ + g * w * O, S[4] = C * O + g * w * _, S[5] = -g * v, S[6] = -g * _ - C * w * O, S[7] = g * O - C * w * _, S[8] = C * v, J.multiply(y, S, t)
            }
        }
    };
    var Xr = new G;
    xr.pointToWindowCoordinates = function (e, t, n, r) {
        return (r = xr.pointToGLWindowCoordinates(e, t, n, r)).y = 2 * t[5] - r.y, r
    }, xr.pointToGLWindowCoordinates = function (e, t, n, r) {
        I.defined(r) || (r = new P.Cartesian2);
        var a = Xr;
        return Q.multiplyByVector(e, G.fromElements(n.x, n.y, n.z, 1, a), a), G.multiplyByScalar(a, 1 / a.w, a), Q.multiplyByVector(t, a, a), P.Cartesian2.fromCartesian4(a, r)
    };
    var Hr = new P.Cartesian3, Jr = new P.Cartesian3, Gr = new P.Cartesian3;
    xr.rotationMatrixFromPositionVelocity = function (e, t, n, r) {
        var a = I.defaultValue(n, P.Ellipsoid.WGS84).geodeticSurfaceNormal(e, Hr), i = P.Cartesian3.cross(t, a, Jr);
        P.Cartesian3.equalsEpsilon(i, P.Cartesian3.ZERO, X.CesiumMath.EPSILON6) && (i = P.Cartesian3.clone(P.Cartesian3.UNIT_X, i));
        var s = P.Cartesian3.cross(i, t, Gr);
        return P.Cartesian3.normalize(s, s), P.Cartesian3.cross(t, s, i), P.Cartesian3.negate(i, i), P.Cartesian3.normalize(i, i), I.defined(r) || (r = new J), r[0] = t.x, r[1] = t.y, r[2] = t.z, r[3] = i.x, r[4] = i.y, r[5] = i.z, r[6] = s.x, r[7] = s.y, r[8] = s.z, r
    };
    var Qr = new Q(0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1), Kr = new P.Cartographic, $r = new P.Cartesian3,
        ea = new P.Cartesian3, ta = new J, na = new Q, ra = new Q;
    xr.basisTo2D = function (e, t, n) {
        var r = Q.getTranslation(t, ea), a = e.ellipsoid, i = a.cartesianToCartographic(r, Kr), s = e.project(i, $r);
        P.Cartesian3.fromElements(s.z, s.x, s.y, s);
        var o = xr.eastNorthUpToFixedFrame(r, a, na), u = Q.inverseTransformation(o, ra), l = Q.getMatrix3(t, ta),
            c = Q.multiplyByMatrix3(u, l, n);
        return Q.multiply(Qr, c, n), Q.setTranslation(n, s, n), n
    }, xr.wgs84To2DModelMatrix = function (e, t, n) {
        var r = e.ellipsoid, a = xr.eastNorthUpToFixedFrame(t, r, na), i = Q.inverseTransformation(a, ra),
            s = r.cartesianToCartographic(t, Kr), o = e.project(s, $r);
        P.Cartesian3.fromElements(o.z, o.x, o.y, o);
        var u = Q.fromTranslation(o, na);
        return Q.multiply(Qr, i, n), Q.multiply(u, n, n), n
    }, e.BoundingSphere = D, e.Cartesian4 = G, e.FeatureDetection = Qe, e.GeographicProjection = t, e.Intersect = s, e.Interval = o, e.Matrix3 = J, e.Matrix4 = Q, e.Quaternion = Ke, e.Resource = Xn, e.Transforms = xr, e.buildModuleUrl = mr
});
