define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./AttributeCompression-6cb5b251", "./createTaskProcessorWorker"], function (e, a, v, y, A, r) {
    "use strict";
    var M = 32767, R = new y.Cartographic, x = new y.Cartesian3, D = new y.Rectangle, E = new y.Ellipsoid,
        F = {min: void 0, max: void 0};
    return r(function (e, a) {
        var r = new Uint16Array(e.positions);
        !function (e) {
            e = new Float64Array(e);
            var a = 0;
            F.min = e[a++], F.max = e[a++], y.Rectangle.unpack(e, a, D), a += y.Rectangle.packedLength, y.Ellipsoid.unpack(e, a, E)
        }(e.packedBuffer);
        var t = D, n = E, i = F.min, s = F.max, o = r.length / 3, c = r.subarray(0, o), u = r.subarray(o, 2 * o),
            p = r.subarray(2 * o, 3 * o);
        A.AttributeCompression.zigZagDeltaDecode(c, u, p);
        for (var f = new Float64Array(r.length), h = 0; h < o; ++h) {
            var l = c[h], d = u[h], m = p[h], C = v.CesiumMath.lerp(t.west, t.east, l / M),
                g = v.CesiumMath.lerp(t.south, t.north, d / M), b = v.CesiumMath.lerp(i, s, m / M),
                w = y.Cartographic.fromRadians(C, g, b, R), k = n.cartographicToCartesian(w, x);
            y.Cartesian3.pack(k, f, 3 * h)
        }
        return a.push(f.buffer), {positions: f.buffer}
    })
});
