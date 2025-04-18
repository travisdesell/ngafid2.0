define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Cartesian2-e875d9d2"], function (e, h, n, i) {
    "use strict";

    function a() {
        this.high = i.Cartesian3.clone(i.Cartesian3.ZERO), this.low = i.Cartesian3.clone(i.Cartesian3.ZERO)
    }

    a.encode = function (e, n) {
        var i;
        return h.defined(n) || (n = {
            high: 0,
            low: 0
        }), 0 <= e ? (i = 65536 * Math.floor(e / 65536), n.high = i, n.low = e - i) : (i = 65536 * Math.floor(-e / 65536), n.high = -i, n.low = e + i), n
    };
    var r = {high: 0, low: 0};
    a.fromCartesian = function (e, n) {
        h.defined(n) || (n = new a);
        var i = n.high, o = n.low;
        return a.encode(e.x, r), i.x = r.high, o.x = r.low, a.encode(e.y, r), i.y = r.high, o.y = r.low, a.encode(e.z, r), i.z = r.high, o.z = r.low, n
    };
    var t = new a;
    a.writeElements = function (e, n, i) {
        a.fromCartesian(e, t);
        var o = t.high, h = t.low;
        n[i] = o.x, n[i + 1] = o.y, n[i + 2] = o.z, n[i + 3] = h.x, n[i + 4] = h.y, n[i + 5] = h.z
    }, e.EncodedCartesian3 = a
});
