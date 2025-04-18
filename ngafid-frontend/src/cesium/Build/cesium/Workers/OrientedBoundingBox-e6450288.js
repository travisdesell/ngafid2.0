define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./Plane-2d882f9f", "./EllipsoidTangentPlane-30395e74"], function (a, q, t, X, j, A, Z, G) {
    "use strict";

    function T(a, t) {
        this.center = j.Cartesian3.clone(q.defaultValue(a, j.Cartesian3.ZERO)), this.halfAxes = A.Matrix3.clone(q.defaultValue(t, A.Matrix3.ZERO))
    }

    T.packedLength = j.Cartesian3.packedLength + A.Matrix3.packedLength, T.pack = function (a, t, e) {
        return e = q.defaultValue(e, 0), j.Cartesian3.pack(a.center, t, e), A.Matrix3.pack(a.halfAxes, t, e + j.Cartesian3.packedLength), t
    }, T.unpack = function (a, t, e) {
        return t = q.defaultValue(t, 0), q.defined(e) || (e = new T), j.Cartesian3.unpack(a, t, e.center), A.Matrix3.unpack(a, t + j.Cartesian3.packedLength, e.halfAxes), e
    };
    var R = new j.Cartesian3, I = new j.Cartesian3, E = new j.Cartesian3, L = new j.Cartesian3, z = new j.Cartesian3,
        S = new j.Cartesian3, U = new A.Matrix3, V = {unitary: new A.Matrix3, diagonal: new A.Matrix3};
    T.fromPoints = function (a, t) {
        if (q.defined(t) || (t = new T), !q.defined(a) || 0 === a.length) return t.halfAxes = A.Matrix3.ZERO, t.center = j.Cartesian3.ZERO, t;
        var e, n = a.length, r = j.Cartesian3.clone(a[0], R);
        for (e = 1; e < n; e++) j.Cartesian3.add(r, a[e], r);
        var i = 1 / n;
        j.Cartesian3.multiplyByScalar(r, i, r);
        var s, o = 0, C = 0, c = 0, u = 0, d = 0, l = 0;
        for (e = 0; e < n; e++) o += (s = j.Cartesian3.subtract(a[e], r, I)).x * s.x, C += s.x * s.y, c += s.x * s.z, u += s.y * s.y, d += s.y * s.z, l += s.z * s.z;
        o *= i, C *= i, c *= i, u *= i, d *= i, l *= i;
        var h = U;
        h[0] = o, h[1] = C, h[2] = c, h[3] = C, h[4] = u, h[5] = d, h[6] = c, h[7] = d, h[8] = l;
        var x = A.Matrix3.computeEigenDecomposition(h, V), M = A.Matrix3.clone(x.unitary, t.halfAxes),
            m = A.Matrix3.getColumn(M, 0, L), f = A.Matrix3.getColumn(M, 1, z), p = A.Matrix3.getColumn(M, 2, S),
            g = -Number.MAX_VALUE, w = -Number.MAX_VALUE, y = -Number.MAX_VALUE, O = Number.MAX_VALUE,
            b = Number.MAX_VALUE, P = Number.MAX_VALUE;
        for (e = 0; e < n; e++) s = a[e], g = Math.max(j.Cartesian3.dot(m, s), g), w = Math.max(j.Cartesian3.dot(f, s), w), y = Math.max(j.Cartesian3.dot(p, s), y), O = Math.min(j.Cartesian3.dot(m, s), O), b = Math.min(j.Cartesian3.dot(f, s), b), P = Math.min(j.Cartesian3.dot(p, s), P);
        m = j.Cartesian3.multiplyByScalar(m, .5 * (O + g), m), f = j.Cartesian3.multiplyByScalar(f, .5 * (b + w), f), p = j.Cartesian3.multiplyByScalar(p, .5 * (P + y), p);
        var N = j.Cartesian3.add(m, f, t.center);
        j.Cartesian3.add(N, p, N);
        var v = E;
        return v.x = g - O, v.y = w - b, v.z = y - P, j.Cartesian3.multiplyByScalar(v, .5, v), A.Matrix3.multiplyByScale(t.halfAxes, v, t.halfAxes), t
    };
    var M = new j.Cartesian3, m = new j.Cartesian3;

    function F(a, t, e, n, r, i, s, o, C, c, u) {
        q.defined(u) || (u = new T);
        var d = u.halfAxes;
        A.Matrix3.setColumn(d, 0, t, d), A.Matrix3.setColumn(d, 1, e, d), A.Matrix3.setColumn(d, 2, n, d);
        var l = M;
        l.x = (r + i) / 2, l.y = (s + o) / 2, l.z = (C + c) / 2;
        var h = m;
        h.x = (i - r) / 2, h.y = (o - s) / 2, h.z = (c - C) / 2;
        var x = u.center;
        return l = A.Matrix3.multiplyByVector(d, l, l), j.Cartesian3.add(a, l, x), A.Matrix3.multiplyByScale(d, h, d), u
    }

    var Y = new j.Cartographic, H = new j.Cartesian3, J = new j.Cartographic, K = new j.Cartographic,
        Q = new j.Cartographic, $ = new j.Cartographic, aa = new j.Cartographic, ta = new j.Cartesian3,
        ea = new j.Cartesian3, na = new j.Cartesian3, ra = new j.Cartesian3, ia = new j.Cartesian3,
        sa = new j.Cartesian2, oa = new j.Cartesian2, Ca = new j.Cartesian2, ca = new j.Cartesian2,
        ua = new j.Cartesian2, da = new j.Cartesian3, la = new j.Cartesian3, ha = new j.Cartesian3,
        xa = new j.Cartesian3, Ma = new j.Cartesian2, ma = new j.Cartesian3, fa = new j.Cartesian3,
        pa = new j.Cartesian3, ga = new Z.Plane(j.Cartesian3.UNIT_X, 0);
    T.fromRectangle = function (a, t, e, n, r) {
        var i, s, o, C, c, u, d;
        if (t = q.defaultValue(t, 0), e = q.defaultValue(e, 0), n = q.defaultValue(n, j.Ellipsoid.WGS84), a.width <= X.CesiumMath.PI) {
            var l = j.Rectangle.center(a, Y), h = n.cartographicToCartesian(l, H),
                x = new G.EllipsoidTangentPlane(h, n);
            d = x.plane;
            var M = l.longitude, m = a.south < 0 && 0 < a.north ? 0 : l.latitude,
                f = j.Cartographic.fromRadians(M, a.north, e, J), p = j.Cartographic.fromRadians(a.west, a.north, e, K),
                g = j.Cartographic.fromRadians(a.west, m, e, Q), w = j.Cartographic.fromRadians(a.west, a.south, e, $),
                y = j.Cartographic.fromRadians(M, a.south, e, aa), O = n.cartographicToCartesian(f, ta),
                b = n.cartographicToCartesian(p, ea), P = n.cartographicToCartesian(g, na),
                N = n.cartographicToCartesian(w, ra), v = n.cartographicToCartesian(y, ia),
                A = x.projectPointToNearestOnPlane(O, sa), T = x.projectPointToNearestOnPlane(b, oa),
                R = x.projectPointToNearestOnPlane(P, Ca), I = x.projectPointToNearestOnPlane(N, ca),
                E = x.projectPointToNearestOnPlane(v, ua);
            return s = -(i = Math.min(T.x, R.x, I.x)), C = Math.max(T.y, A.y), o = Math.min(I.y, E.y), p.height = w.height = t, b = n.cartographicToCartesian(p, ea), N = n.cartographicToCartesian(w, ra), c = Math.min(Z.Plane.getPointDistance(d, b), Z.Plane.getPointDistance(d, N)), u = e, F(x.origin, x.xAxis, x.yAxis, x.zAxis, i, s, o, C, c, u, r)
        }
        var L = 0 < a.south, z = a.north < 0, S = L ? a.south : z ? a.north : 0, U = j.Rectangle.center(a, Y).longitude,
            V = j.Cartesian3.fromRadians(U, S, e, n, da);
        V.z = 0;
        var B = Math.abs(V.x) < X.CesiumMath.EPSILON10 && Math.abs(V.y) < X.CesiumMath.EPSILON10 ? j.Cartesian3.UNIT_X : j.Cartesian3.normalize(V, la),
            _ = j.Cartesian3.UNIT_Z, k = j.Cartesian3.cross(B, _, ha);
        d = Z.Plane.fromPointNormal(V, B, ga);
        var W = j.Cartesian3.fromRadians(U + X.CesiumMath.PI_OVER_TWO, S, e, n, xa);
        i = -(s = j.Cartesian3.dot(Z.Plane.projectPointOntoPlane(d, W, Ma), k)), C = j.Cartesian3.fromRadians(0, a.north, z ? t : e, n, ma).z, o = j.Cartesian3.fromRadians(0, a.south, L ? t : e, n, fa).z;
        var D = j.Cartesian3.fromRadians(a.east, S, e, n, pa);
        return F(V, k, _, B, i, s, o, C, c = Z.Plane.getPointDistance(d, D), u = 0, r)
    }, T.clone = function (a, t) {
        if (q.defined(a)) return q.defined(t) ? (j.Cartesian3.clone(a.center, t.center), A.Matrix3.clone(a.halfAxes, t.halfAxes), t) : new T(a.center, a.halfAxes)
    }, T.intersectPlane = function (a, t) {
        var e = a.center, n = t.normal, r = a.halfAxes, i = n.x, s = n.y, o = n.z,
            C = Math.abs(i * r[A.Matrix3.COLUMN0ROW0] + s * r[A.Matrix3.COLUMN0ROW1] + o * r[A.Matrix3.COLUMN0ROW2]) + Math.abs(i * r[A.Matrix3.COLUMN1ROW0] + s * r[A.Matrix3.COLUMN1ROW1] + o * r[A.Matrix3.COLUMN1ROW2]) + Math.abs(i * r[A.Matrix3.COLUMN2ROW0] + s * r[A.Matrix3.COLUMN2ROW1] + o * r[A.Matrix3.COLUMN2ROW2]),
            c = j.Cartesian3.dot(n, e) + t.distance;
        return c <= -C ? A.Intersect.OUTSIDE : C <= c ? A.Intersect.INSIDE : A.Intersect.INTERSECTING
    };
    var x = new j.Cartesian3, f = new j.Cartesian3, p = new j.Cartesian3, h = new j.Cartesian3;
    T.distanceSquaredTo = function (a, t) {
        var e = j.Cartesian3.subtract(t, a.center, M), n = a.halfAxes, r = A.Matrix3.getColumn(n, 0, x),
            i = A.Matrix3.getColumn(n, 1, f), s = A.Matrix3.getColumn(n, 2, p), o = j.Cartesian3.magnitude(r),
            C = j.Cartesian3.magnitude(i), c = j.Cartesian3.magnitude(s);
        j.Cartesian3.normalize(r, r), j.Cartesian3.normalize(i, i), j.Cartesian3.normalize(s, s);
        var u = h;
        u.x = j.Cartesian3.dot(e, r), u.y = j.Cartesian3.dot(e, i), u.z = j.Cartesian3.dot(e, s);
        var d, l = 0;
        return u.x < -o ? l += (d = u.x + o) * d : u.x > o && (l += (d = u.x - o) * d), u.y < -C ? l += (d = u.y + C) * d : u.y > C && (l += (d = u.y - C) * d), u.z < -c ? l += (d = u.z + c) * d : u.z > c && (l += (d = u.z - c) * d), l
    };
    var g = new j.Cartesian3, w = new j.Cartesian3;
    T.computePlaneDistances = function (a, t, e, n) {
        q.defined(n) || (n = new A.Interval);
        var r = Number.POSITIVE_INFINITY, i = Number.NEGATIVE_INFINITY, s = a.center, o = a.halfAxes,
            C = A.Matrix3.getColumn(o, 0, x), c = A.Matrix3.getColumn(o, 1, f), u = A.Matrix3.getColumn(o, 2, p),
            d = j.Cartesian3.add(C, c, g);
        j.Cartesian3.add(d, u, d), j.Cartesian3.add(d, s, d);
        var l = j.Cartesian3.subtract(d, t, w), h = j.Cartesian3.dot(e, l);
        return r = Math.min(h, r), i = Math.max(h, i), j.Cartesian3.add(s, C, d), j.Cartesian3.add(d, c, d), j.Cartesian3.subtract(d, u, d), j.Cartesian3.subtract(d, t, l), h = j.Cartesian3.dot(e, l), r = Math.min(h, r), i = Math.max(h, i), j.Cartesian3.add(s, C, d), j.Cartesian3.subtract(d, c, d), j.Cartesian3.add(d, u, d), j.Cartesian3.subtract(d, t, l), h = j.Cartesian3.dot(e, l), r = Math.min(h, r), i = Math.max(h, i), j.Cartesian3.add(s, C, d), j.Cartesian3.subtract(d, c, d), j.Cartesian3.subtract(d, u, d), j.Cartesian3.subtract(d, t, l), h = j.Cartesian3.dot(e, l), r = Math.min(h, r), i = Math.max(h, i), j.Cartesian3.subtract(s, C, d), j.Cartesian3.add(d, c, d), j.Cartesian3.add(d, u, d), j.Cartesian3.subtract(d, t, l), h = j.Cartesian3.dot(e, l), r = Math.min(h, r), i = Math.max(h, i), j.Cartesian3.subtract(s, C, d), j.Cartesian3.add(d, c, d), j.Cartesian3.subtract(d, u, d), j.Cartesian3.subtract(d, t, l), h = j.Cartesian3.dot(e, l), r = Math.min(h, r), i = Math.max(h, i), j.Cartesian3.subtract(s, C, d), j.Cartesian3.subtract(d, c, d), j.Cartesian3.add(d, u, d), j.Cartesian3.subtract(d, t, l), h = j.Cartesian3.dot(e, l), r = Math.min(h, r), i = Math.max(h, i), j.Cartesian3.subtract(s, C, d), j.Cartesian3.subtract(d, c, d), j.Cartesian3.subtract(d, u, d), j.Cartesian3.subtract(d, t, l), h = j.Cartesian3.dot(e, l), r = Math.min(h, r), i = Math.max(h, i), n.start = r, n.stop = i, n
    };
    var n = new A.BoundingSphere;
    T.isOccluded = function (a, t) {
        var e = A.BoundingSphere.fromOrientedBoundingBox(a, n);
        return !t.isBoundingSphereVisible(e)
    }, T.prototype.intersectPlane = function (a) {
        return T.intersectPlane(this, a)
    }, T.prototype.distanceSquaredTo = function (a) {
        return T.distanceSquaredTo(this, a)
    }, T.prototype.computePlaneDistances = function (a, t, e) {
        return T.computePlaneDistances(this, a, t, e)
    }, T.prototype.isOccluded = function (a) {
        return T.isOccluded(this, a)
    }, T.equals = function (a, t) {
        return a === t || q.defined(a) && q.defined(t) && j.Cartesian3.equals(a.center, t.center) && A.Matrix3.equals(a.halfAxes, t.halfAxes)
    }, T.prototype.clone = function (a) {
        return T.clone(this, a)
    }, T.prototype.equals = function (a) {
        return T.equals(this, a)
    }, a.OrientedBoundingBox = T
});
