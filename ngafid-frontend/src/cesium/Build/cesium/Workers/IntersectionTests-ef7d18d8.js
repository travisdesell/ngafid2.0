define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02"], function (a, q, t, z, T, U) {
    "use strict";
    var W = {};

    function C(a, t, e) {
        var r = a + t;
        return z.CesiumMath.sign(a) !== z.CesiumMath.sign(t) && Math.abs(r / Math.max(Math.abs(a), Math.abs(t))) < e ? 0 : r
    }

    W.computeDiscriminant = function (a, t, e) {
        return t * t - 4 * a * e
    }, W.computeRealRoots = function (a, t, e) {
        var r;
        if (0 === a) return 0 === t ? [] : [-e / t];
        if (0 === t) {
            if (0 === e) return [0, 0];
            var n = Math.abs(e), i = Math.abs(a);
            if (n < i && n / i < z.CesiumMath.EPSILON14) return [0, 0];
            if (i < n && i / n < z.CesiumMath.EPSILON14) return [];
            if ((r = -e / a) < 0) return [];
            var s = Math.sqrt(r);
            return [-s, s]
        }
        if (0 === e) return (r = -t / a) < 0 ? [r, 0] : [0, r];
        var o = C(t * t, -(4 * a * e), z.CesiumMath.EPSILON14);
        if (o < 0) return [];
        var u = -.5 * C(t, z.CesiumMath.sign(t) * Math.sqrt(o), z.CesiumMath.EPSILON14);
        return 0 < t ? [u / a, e / u] : [e / u, u / a]
    };
    var b = {};

    function o(a, t, e, r) {
        var n, i, s = a, o = t / 3, u = e / 3, C = r, c = s * u, l = o * C, h = o * o, M = u * u, f = s * u - h,
            m = s * C - o * u, d = o * C - M, v = 4 * f * d - m * m;
        if (v < 0) {
            var g, p, w,
                R = -((w = c * M <= h * l ? -2 * o * (p = f) + (g = s) * m : -(g = C) * m + 2 * u * (p = d)) < 0 ? -1 : 1) * Math.abs(g) * Math.sqrt(-v),
                S = (i = R - w) / 2, O = S < 0 ? -Math.pow(-S, 1 / 3) : Math.pow(S, 1 / 3), x = i === R ? -O : -p / O;
            return n = p <= 0 ? O + x : -w / (O * O + x * x + p), c * M <= h * l ? [(n - o) / s] : [-C / (n + u)]
        }
        var y = f, P = -2 * o * f + s * m, N = d, b = -C * m + 2 * u * d, q = Math.sqrt(v), L = Math.sqrt(3) / 2,
            I = Math.abs(Math.atan2(s * q, -P) / 3);
        n = 2 * Math.sqrt(-y);
        var E = Math.cos(I);
        i = n * E;
        var z = n * (-E / 2 - L * Math.sin(I)), T = 2 * o < i + z ? i - o : z - o, U = s, W = T / U;
        I = Math.abs(Math.atan2(C * q, -b) / 3);
        var B = -C,
            V = (i = (n = 2 * Math.sqrt(-N)) * (E = Math.cos(I))) + (z = n * (-E / 2 - L * Math.sin(I))) < 2 * u ? i + u : z + u,
            Z = B / V, A = -T * V - U * B, D = (u * A - o * (T * B)) / (-o * A + u * (U * V));
        return W <= D ? W <= Z ? D <= Z ? [W, D, Z] : [W, Z, D] : [Z, W, D] : W <= Z ? [D, W, Z] : D <= Z ? [D, Z, W] : [Z, D, W]
    }

    b.computeDiscriminant = function (a, t, e, r) {
        var n = t * t, i = e * e;
        return 18 * a * t * e * r + n * i - 27 * (a * a) * (r * r) - 4 * (a * i * e + n * t * r)
    }, b.computeRealRoots = function (a, t, e, r) {
        var n, i;
        if (0 === a) return W.computeRealRoots(t, e, r);
        if (0 !== t) return 0 === e ? 0 === r ? (i = -t / a) < 0 ? [i, 0, 0] : [0, 0, i] : o(a, t, 0, r) : 0 === r ? 0 === (n = W.computeRealRoots(a, t, e)).length ? [0] : n[1] <= 0 ? [n[0], n[1], 0] : 0 <= n[0] ? [0, n[0], n[1]] : [n[0], 0, n[1]] : o(a, t, e, r);
        if (0 !== e) return 0 === r ? 0 === (n = W.computeRealRoots(a, 0, e)).Length ? [0] : [n[0], 0, n[1]] : o(a, 0, e, r);
        if (0 === r) return [0, 0, 0];
        var s = (i = -r / a) < 0 ? -Math.pow(-i, 1 / 3) : Math.pow(i, 1 / 3);
        return [s, s, s]
    };
    var B = {};

    function c(a, t, e, r) {
        var n = a * a, i = t - 3 * n / 8, s = e - t * a / 2 + n * a / 8,
            o = r - e * a / 4 + t * n / 16 - 3 * n * n / 256, u = b.computeRealRoots(1, 2 * i, i * i - 4 * o, -s * s);
        if (0 < u.length) {
            var C = -a / 4, c = u[u.length - 1];
            if (Math.abs(c) < z.CesiumMath.EPSILON14) {
                var l = W.computeRealRoots(1, i, o);
                if (2 === l.length) {
                    var h, M = l[0], f = l[1];
                    if (0 <= M && 0 <= f) {
                        var m = Math.sqrt(M), d = Math.sqrt(f);
                        return [C - d, C - m, C + m, C + d]
                    }
                    if (0 <= M && f < 0) return [C - (h = Math.sqrt(M)), C + h];
                    if (M < 0 && 0 <= f) return [C - (h = Math.sqrt(f)), C + h]
                }
                return []
            }
            if (0 < c) {
                var v = Math.sqrt(c), g = (i + c - s / v) / 2, p = (i + c + s / v) / 2, w = W.computeRealRoots(1, v, g),
                    R = W.computeRealRoots(1, -v, p);
                return 0 !== w.length ? (w[0] += C, w[1] += C, 0 !== R.length ? (R[0] += C, R[1] += C, w[1] <= R[0] ? [w[0], w[1], R[0], R[1]] : R[1] <= w[0] ? [R[0], R[1], w[0], w[1]] : w[0] >= R[0] && w[1] <= R[1] ? [R[0], w[0], w[1], R[1]] : R[0] >= w[0] && R[1] <= w[1] ? [w[0], R[0], R[1], w[1]] : w[0] > R[0] && w[0] < R[1] ? [R[0], w[0], R[1], w[1]] : [w[0], R[0], w[1], R[1]]) : w) : 0 !== R.length ? (R[0] += C, R[1] += C, R) : []
            }
        }
        return []
    }

    function l(a, t, e, r) {
        var n = a * a, i = -2 * t, s = e * a + t * t - 4 * r, o = n * r - e * t * a + e * e,
            u = b.computeRealRoots(1, i, s, o);
        if (0 < u.length) {
            var C, c, l, h, M, f, m = u[0], d = t - m, v = d * d, g = a / 2, p = d / 2, w = v - 4 * r,
                R = v + 4 * Math.abs(r), S = n - 4 * m, O = n + 4 * Math.abs(m);
            if (m < 0 || w * O < S * R) {
                var x = Math.sqrt(S);
                C = x / 2, c = 0 === x ? 0 : (a * p - e) / x
            } else {
                var y = Math.sqrt(w);
                C = 0 === y ? 0 : (a * p - e) / y, c = y / 2
            }
            0 == g && 0 === C ? h = l = 0 : z.CesiumMath.sign(g) === z.CesiumMath.sign(C) ? h = m / (l = g + C) : l = m / (h = g - C), 0 == p && 0 === c ? f = M = 0 : z.CesiumMath.sign(p) === z.CesiumMath.sign(c) ? f = r / (M = p + c) : M = r / (f = p - c);
            var P = W.computeRealRoots(1, l, M), N = W.computeRealRoots(1, h, f);
            if (0 !== P.length) return 0 !== N.length ? P[1] <= N[0] ? [P[0], P[1], N[0], N[1]] : N[1] <= P[0] ? [N[0], N[1], P[0], P[1]] : P[0] >= N[0] && P[1] <= N[1] ? [N[0], P[0], P[1], N[1]] : N[0] >= P[0] && N[1] <= P[1] ? [P[0], N[0], N[1], P[1]] : P[0] > N[0] && P[0] < N[1] ? [N[0], P[0], N[1], P[1]] : [P[0], N[0], P[1], N[1]] : P;
            if (0 !== N.length) return N
        }
        return []
    }

    function e(a, t) {
        t = T.Cartesian3.clone(q.defaultValue(t, T.Cartesian3.ZERO)), T.Cartesian3.equals(t, T.Cartesian3.ZERO) || T.Cartesian3.normalize(t, t), this.origin = T.Cartesian3.clone(q.defaultValue(a, T.Cartesian3.ZERO)), this.direction = t
    }

    B.computeDiscriminant = function (a, t, e, r, n) {
        var i = a * a, s = t * t, o = s * t, u = e * e, C = u * e, c = r * r, l = c * r, h = n * n;
        return s * u * c - 4 * o * l - 4 * a * C * c + 18 * a * t * e * l - 27 * i * c * c + 256 * (i * a) * (h * n) + n * (18 * o * e * r - 4 * s * C + 16 * a * u * u - 80 * a * t * u * r - 6 * a * s * c + 144 * i * e * c) + h * (144 * a * s * e - 27 * s * s - 128 * i * u - 192 * i * t * r)
    }, B.computeRealRoots = function (a, t, e, r, n) {
        if (Math.abs(a) < z.CesiumMath.EPSILON15) return b.computeRealRoots(t, e, r, n);
        var i = t / a, s = e / a, o = r / a, u = n / a, C = i < 0 ? 1 : 0;
        switch (C += s < 0 ? C + 1 : C, C += o < 0 ? C + 1 : C, C += u < 0 ? C + 1 : C) {
            case 0:
                return c(i, s, o, u);
            case 1:
            case 2:
                return l(i, s, o, u);
            case 3:
            case 4:
                return c(i, s, o, u);
            case 5:
                return l(i, s, o, u);
            case 6:
            case 7:
                return c(i, s, o, u);
            case 8:
                return l(i, s, o, u);
            case 9:
            case 10:
                return c(i, s, o, u);
            case 11:
                return l(i, s, o, u);
            case 12:
            case 13:
            case 14:
            case 15:
                return c(i, s, o, u);
            default:
                return
        }
    }, e.clone = function (a, t) {
        if (q.defined(a)) return q.defined(t) ? (t.origin = T.Cartesian3.clone(a.origin), t.direction = T.Cartesian3.clone(a.direction), t) : new e(a.origin, a.direction)
    }, e.getPoint = function (a, t, e) {
        return q.defined(e) || (e = new T.Cartesian3), e = T.Cartesian3.multiplyByScalar(a.direction, t, e), T.Cartesian3.add(a.origin, e, e)
    };
    var h = {
        rayPlane: function (a, t, e) {
            q.defined(e) || (e = new T.Cartesian3);
            var r = a.origin, n = a.direction, i = t.normal, s = T.Cartesian3.dot(i, n);
            if (!(Math.abs(s) < z.CesiumMath.EPSILON15)) {
                var o = (-t.distance - T.Cartesian3.dot(i, r)) / s;
                if (!(o < 0)) return e = T.Cartesian3.multiplyByScalar(n, o, e), T.Cartesian3.add(r, e, e)
            }
        }
    }, v = new T.Cartesian3, g = new T.Cartesian3, p = new T.Cartesian3, w = new T.Cartesian3, R = new T.Cartesian3;
    h.rayTriangleParametric = function (a, t, e, r, n) {
        n = q.defaultValue(n, !1);
        var i, s, o, u, C, c = a.origin, l = a.direction, h = T.Cartesian3.subtract(e, t, v),
            M = T.Cartesian3.subtract(r, t, g), f = T.Cartesian3.cross(l, M, p), m = T.Cartesian3.dot(h, f);
        if (n) {
            if (m < z.CesiumMath.EPSILON6) return;
            if (i = T.Cartesian3.subtract(c, t, w), (o = T.Cartesian3.dot(i, f)) < 0 || m < o) return;
            if (s = T.Cartesian3.cross(i, h, R), (u = T.Cartesian3.dot(l, s)) < 0 || m < o + u) return;
            C = T.Cartesian3.dot(M, s) / m
        } else {
            if (Math.abs(m) < z.CesiumMath.EPSILON6) return;
            var d = 1 / m;
            if (i = T.Cartesian3.subtract(c, t, w), (o = T.Cartesian3.dot(i, f) * d) < 0 || 1 < o) return;
            if (s = T.Cartesian3.cross(i, h, R), (u = T.Cartesian3.dot(l, s) * d) < 0 || 1 < o + u) return;
            C = T.Cartesian3.dot(M, s) * d
        }
        return C
    }, h.rayTriangle = function (a, t, e, r, n, i) {
        var s = h.rayTriangleParametric(a, t, e, r, n);
        if (q.defined(s) && !(s < 0)) return q.defined(i) || (i = new T.Cartesian3), T.Cartesian3.multiplyByScalar(a.direction, s, i), T.Cartesian3.add(a.origin, i, i)
    };
    var M = new e;
    h.lineSegmentTriangle = function (a, t, e, r, n, i, s) {
        var o = M;
        T.Cartesian3.clone(a, o.origin), T.Cartesian3.subtract(t, a, o.direction), T.Cartesian3.normalize(o.direction, o.direction);
        var u = h.rayTriangleParametric(o, e, r, n, i);
        if (!(!q.defined(u) || u < 0 || u > T.Cartesian3.distance(a, t))) return q.defined(s) || (s = new T.Cartesian3), T.Cartesian3.multiplyByScalar(o.direction, u, s), T.Cartesian3.add(o.origin, s, s)
    };
    var f = {root0: 0, root1: 0};

    function u(a, t, e) {
        q.defined(e) || (e = new U.Interval);
        var r = a.origin, n = a.direction, i = t.center, s = t.radius * t.radius, o = T.Cartesian3.subtract(r, i, p),
            u = function (a, t, e, r) {
                var n = t * t - 4 * a * e;
                if (!(n < 0)) {
                    if (0 < n) {
                        var i = 1 / (2 * a), s = Math.sqrt(n), o = (-t + s) * i, u = (-t - s) * i;
                        return o < u ? (r.root0 = o, r.root1 = u) : (r.root0 = u, r.root1 = o), r
                    }
                    var C = -t / (2 * a);
                    if (0 != C) return r.root0 = r.root1 = C, r
                }
            }(T.Cartesian3.dot(n, n), 2 * T.Cartesian3.dot(n, o), T.Cartesian3.magnitudeSquared(o) - s, f);
        if (q.defined(u)) return e.start = u.root0, e.stop = u.root1, e
    }

    h.raySphere = function (a, t, e) {
        if (e = u(a, t, e), q.defined(e) && !(e.stop < 0)) return e.start = Math.max(e.start, 0), e
    };
    var m = new e;
    h.lineSegmentSphere = function (a, t, e, r) {
        var n = m;
        T.Cartesian3.clone(a, n.origin);
        var i = T.Cartesian3.subtract(t, a, n.direction), s = T.Cartesian3.magnitude(i);
        if (T.Cartesian3.normalize(i, i), r = u(n, e, r), !(!q.defined(r) || r.stop < 0 || r.start > s)) return r.start = Math.max(r.start, 0), r.stop = Math.min(r.stop, s), r
    };
    var d = new T.Cartesian3, S = new T.Cartesian3;

    function V(a, t, e) {
        var r = a + t;
        return z.CesiumMath.sign(a) !== z.CesiumMath.sign(t) && Math.abs(r / Math.max(Math.abs(a), Math.abs(t))) < e ? 0 : r
    }

    h.rayEllipsoid = function (a, t) {
        var e, r, n, i, s, o = t.oneOverRadii, u = T.Cartesian3.multiplyComponents(o, a.origin, d),
            C = T.Cartesian3.multiplyComponents(o, a.direction, S), c = T.Cartesian3.magnitudeSquared(u),
            l = T.Cartesian3.dot(u, C);
        if (1 < c) {
            if (0 <= l) return;
            var h = l * l;
            if (e = c - 1, h < (n = (r = T.Cartesian3.magnitudeSquared(C)) * e)) return;
            if (n < h) {
                i = l * l - n;
                var M = (s = -l + Math.sqrt(i)) / r, f = e / s;
                return M < f ? new U.Interval(M, f) : {start: f, stop: M}
            }
            var m = Math.sqrt(e / r);
            return new U.Interval(m, m)
        }
        return c < 1 ? (e = c - 1, i = l * l - (n = (r = T.Cartesian3.magnitudeSquared(C)) * e), s = -l + Math.sqrt(i), new U.Interval(0, s / r)) : l < 0 ? (r = T.Cartesian3.magnitudeSquared(C), new U.Interval(0, -l / r)) : void 0
    };
    var L = new T.Cartesian3, I = new T.Cartesian3, E = new T.Cartesian3, Z = new T.Cartesian3, A = new T.Cartesian3,
        D = new U.Matrix3, k = new U.Matrix3, F = new U.Matrix3, G = new U.Matrix3, Y = new U.Matrix3,
        _ = new U.Matrix3, j = new U.Matrix3, H = new T.Cartesian3, J = new T.Cartesian3, K = new T.Cartographic;
    h.grazingAltitudeLocation = function (a, t) {
        var e = a.origin, r = a.direction;
        if (!T.Cartesian3.equals(e, T.Cartesian3.ZERO)) {
            var n = t.geodeticSurfaceNormal(e, L);
            if (0 <= T.Cartesian3.dot(r, n)) return e
        }
        var i = q.defined(this.rayEllipsoid(a, t)), s = t.transformPositionToScaledSpace(r, L),
            o = T.Cartesian3.normalize(s, s), u = T.Cartesian3.mostOrthogonalAxis(s, Z),
            C = T.Cartesian3.normalize(T.Cartesian3.cross(u, o, I), I),
            c = T.Cartesian3.normalize(T.Cartesian3.cross(o, C, E), E), l = D;
        l[0] = o.x, l[1] = o.y, l[2] = o.z, l[3] = C.x, l[4] = C.y, l[5] = C.z, l[6] = c.x, l[7] = c.y, l[8] = c.z;
        var h = U.Matrix3.transpose(l, k), M = U.Matrix3.fromScale(t.radii, F),
            f = U.Matrix3.fromScale(t.oneOverRadii, G), m = Y;
        m[0] = 0, m[1] = -r.z, m[2] = r.y, m[3] = r.z, m[4] = 0, m[5] = -r.x, m[6] = -r.y, m[7] = r.x, m[8] = 0;
        var d, v, g = U.Matrix3.multiply(U.Matrix3.multiply(h, f, _), m, _),
            p = U.Matrix3.multiply(U.Matrix3.multiply(g, M, j), l, j), w = U.Matrix3.multiplyByVector(g, e, A),
            R = function (a, t, e, r, n) {
                var i, s = r * r, o = n * n, u = (a[U.Matrix3.COLUMN1ROW1] - a[U.Matrix3.COLUMN2ROW2]) * o,
                    C = n * (r * V(a[U.Matrix3.COLUMN1ROW0], a[U.Matrix3.COLUMN0ROW1], z.CesiumMath.EPSILON15) + t.y),
                    c = a[U.Matrix3.COLUMN0ROW0] * s + a[U.Matrix3.COLUMN2ROW2] * o + r * t.x + e,
                    l = o * V(a[U.Matrix3.COLUMN2ROW1], a[U.Matrix3.COLUMN1ROW2], z.CesiumMath.EPSILON15),
                    h = n * (r * V(a[U.Matrix3.COLUMN2ROW0], a[U.Matrix3.COLUMN0ROW2]) + t.z), M = [];
                if (0 == h && 0 == l) {
                    if (0 === (i = W.computeRealRoots(u, C, c)).length) return M;
                    var f = i[0], m = Math.sqrt(Math.max(1 - f * f, 0));
                    if (M.push(new T.Cartesian3(r, n * f, n * -m)), M.push(new T.Cartesian3(r, n * f, n * m)), 2 === i.length) {
                        var d = i[1], v = Math.sqrt(Math.max(1 - d * d, 0));
                        M.push(new T.Cartesian3(r, n * d, n * -v)), M.push(new T.Cartesian3(r, n * d, n * v))
                    }
                    return M
                }
                var g = h * h, p = l * l, w = h * l, R = u * u + p, S = 2 * (C * u + w), O = 2 * c * u + C * C - p + g,
                    x = 2 * (c * C - w), y = c * c - g;
                if (0 == R && 0 == S && 0 == O && 0 == x) return M;
                var P = (i = B.computeRealRoots(R, S, O, x, y)).length;
                if (0 === P) return M;
                for (var N = 0; N < P; ++N) {
                    var b = i[N], q = b * b, L = Math.max(1 - q, 0), I = Math.sqrt(L),
                        E = (z.CesiumMath.sign(u) === z.CesiumMath.sign(c) ? V(u * q + c, C * b, z.CesiumMath.EPSILON12) : z.CesiumMath.sign(c) === z.CesiumMath.sign(C * b) ? V(u * q, C * b + c, z.CesiumMath.EPSILON12) : V(u * q + C * b, c, z.CesiumMath.EPSILON12)) * V(l * b, h, z.CesiumMath.EPSILON15);
                    E < 0 ? M.push(new T.Cartesian3(r, n * b, n * I)) : 0 < E ? M.push(new T.Cartesian3(r, n * b, n * -I)) : 0 !== I ? (M.push(new T.Cartesian3(r, n * b, n * -I)), M.push(new T.Cartesian3(r, n * b, n * I)), ++N) : M.push(new T.Cartesian3(r, n * b, n * I))
                }
                return M
            }(p, T.Cartesian3.negate(w, L), 0, 0, 1), S = R.length;
        if (0 < S) {
            for (var O = T.Cartesian3.clone(T.Cartesian3.ZERO, J), x = Number.NEGATIVE_INFINITY, y = 0; y < S; ++y) {
                d = U.Matrix3.multiplyByVector(M, U.Matrix3.multiplyByVector(l, R[y], H), H);
                var P = T.Cartesian3.normalize(T.Cartesian3.subtract(d, e, Z), Z), N = T.Cartesian3.dot(P, r);
                x < N && (x = N, O = T.Cartesian3.clone(d, O))
            }
            var b = t.cartesianToCartographic(O, K);
            return x = z.CesiumMath.clamp(x, 0, 1), v = T.Cartesian3.magnitude(T.Cartesian3.subtract(O, e, Z)) * Math.sqrt(1 - x * x), v = i ? -v : v, b.height = v, t.cartographicToCartesian(b, new T.Cartesian3)
        }
    };
    var O = new T.Cartesian3;
    h.lineSegmentPlane = function (a, t, e, r) {
        q.defined(r) || (r = new T.Cartesian3);
        var n = T.Cartesian3.subtract(t, a, O), i = e.normal, s = T.Cartesian3.dot(i, n);
        if (!(Math.abs(s) < z.CesiumMath.EPSILON6)) {
            var o = T.Cartesian3.dot(i, a), u = -(e.distance + o) / s;
            if (!(u < 0 || 1 < u)) return T.Cartesian3.multiplyByScalar(n, u, r), T.Cartesian3.add(a, r, r), r
        }
    }, h.trianglePlaneIntersection = function (a, t, e, r) {
        var n, i, s = r.normal, o = r.distance, u = T.Cartesian3.dot(s, a) + o < 0, C = T.Cartesian3.dot(s, t) + o < 0,
            c = T.Cartesian3.dot(s, e) + o < 0, l = 0;
        if (l += u ? 1 : 0, l += C ? 1 : 0, 1 != (l += c ? 1 : 0) && 2 != l || (n = new T.Cartesian3, i = new T.Cartesian3), 1 == l) {
            if (u) return h.lineSegmentPlane(a, t, r, n), h.lineSegmentPlane(a, e, r, i), {
                positions: [a, t, e, n, i],
                indices: [0, 3, 4, 1, 2, 4, 1, 4, 3]
            };
            if (C) return h.lineSegmentPlane(t, e, r, n), h.lineSegmentPlane(t, a, r, i), {
                positions: [a, t, e, n, i],
                indices: [1, 3, 4, 2, 0, 4, 2, 4, 3]
            };
            if (c) return h.lineSegmentPlane(e, a, r, n), h.lineSegmentPlane(e, t, r, i), {
                positions: [a, t, e, n, i],
                indices: [2, 3, 4, 0, 1, 4, 0, 4, 3]
            }
        } else if (2 == l) {
            if (!u) return h.lineSegmentPlane(t, a, r, n), h.lineSegmentPlane(e, a, r, i), {
                positions: [a, t, e, n, i],
                indices: [1, 2, 4, 1, 4, 3, 0, 3, 4]
            };
            if (!C) return h.lineSegmentPlane(e, t, r, n), h.lineSegmentPlane(a, t, r, i), {
                positions: [a, t, e, n, i],
                indices: [2, 0, 4, 2, 4, 3, 1, 3, 4]
            };
            if (!c) return h.lineSegmentPlane(a, e, r, n), h.lineSegmentPlane(t, e, r, i), {
                positions: [a, t, e, n, i],
                indices: [0, 1, 4, 0, 4, 3, 2, 3, 4]
            }
        }
    }, a.IntersectionTests = h, a.Ray = e
});
