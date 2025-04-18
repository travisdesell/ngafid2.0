define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2"], function (t, u, o, r, c) {
    "use strict";
    var s = {
        octEncodeInRange: function (t, o, e) {
            if (e.x = t.x / (Math.abs(t.x) + Math.abs(t.y) + Math.abs(t.z)), e.y = t.y / (Math.abs(t.x) + Math.abs(t.y) + Math.abs(t.z)), t.z < 0) {
                var n = e.x, a = e.y;
                e.x = (1 - Math.abs(a)) * r.CesiumMath.signNotZero(n), e.y = (1 - Math.abs(n)) * r.CesiumMath.signNotZero(a)
            }
            return e.x = r.CesiumMath.toSNorm(e.x, o), e.y = r.CesiumMath.toSNorm(e.y, o), e
        }, octEncode: function (t, o) {
            return s.octEncodeInRange(t, 255, o)
        }
    }, e = new c.Cartesian2, n = new Uint8Array(1);

    function a(t) {
        return n[0] = t, n[0]
    }

    s.octEncodeToCartesian4 = function (t, o) {
        return s.octEncodeInRange(t, 65535, e), o.x = a(e.x * (1 / 256)), o.y = a(e.x), o.z = a(e.y * (1 / 256)), o.w = a(e.y), o
    }, s.octDecodeInRange = function (t, o, e, n) {
        if (n.x = r.CesiumMath.fromSNorm(t, e), n.y = r.CesiumMath.fromSNorm(o, e), n.z = 1 - (Math.abs(n.x) + Math.abs(n.y)), n.z < 0) {
            var a = n.x;
            n.x = (1 - Math.abs(n.y)) * r.CesiumMath.signNotZero(a), n.y = (1 - Math.abs(a)) * r.CesiumMath.signNotZero(n.y)
        }
        return c.Cartesian3.normalize(n, n)
    }, s.octDecode = function (t, o, e) {
        return s.octDecodeInRange(t, o, 255, e)
    }, s.octDecodeFromCartesian4 = function (t, o) {
        var e = 256 * t.x + t.y, n = 256 * t.z + t.w;
        return s.octDecodeInRange(e, n, 65535, o)
    }, s.octPackFloat = function (t) {
        return 256 * t.x + t.y
    };
    var i = new c.Cartesian2;

    function d(t) {
        return t >> 1 ^ -(1 & t)
    }

    s.octEncodeFloat = function (t) {
        return s.octEncode(t, i), s.octPackFloat(i)
    }, s.octDecodeFloat = function (t, o) {
        var e = t / 256, n = Math.floor(e), a = 256 * (e - n);
        return s.octDecode(n, a, o)
    }, s.octPack = function (t, o, e, n) {
        var a = s.octEncodeFloat(t), r = s.octEncodeFloat(o), c = s.octEncode(e, i);
        return n.x = 65536 * c.x + a, n.y = 65536 * c.y + r, n
    }, s.octUnpack = function (t, o, e, n) {
        var a = t.x / 65536, r = Math.floor(a), c = 65536 * (a - r);
        a = t.y / 65536;
        var i = Math.floor(a), u = 65536 * (a - i);
        s.octDecodeFloat(c, o), s.octDecodeFloat(u, e), s.octDecode(r, i, n)
    }, s.compressTextureCoordinates = function (t) {
        return 4096 * (4095 * t.x | 0) + (4095 * t.y | 0)
    }, s.decompressTextureCoordinates = function (t, o) {
        var e = t / 4096, n = Math.floor(e);
        return o.x = n / 4095, o.y = (t - 4096 * n) / 4095, o
    }, s.zigZagDeltaDecode = function (t, o, e) {
        for (var n = t.length, a = 0, r = 0, c = 0, i = 0; i < n; ++i) a += d(t[i]), r += d(o[i]), t[i] = a, o[i] = r, u.defined(e) && (c += d(e[i]), e[i] = c)
    }, t.AttributeCompression = s
});
