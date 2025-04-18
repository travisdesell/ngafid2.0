define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./EllipsoidRhumbLine-5134246a"], function (e, T, t, W, P, n, I, B, N) {
    "use strict";

    function r(e, t, n) {
        n = n || 2;
        var r, a, i, x, u, o, s, p = t && t.length, h = p ? t[0] * n : e.length, f = l(e, 0, h, n, !0), y = [];
        if (!f || f.next === f.prev) return y;
        if (p && (f = function (e, t, n, r) {
            var a, i, x, u, o, s = [];
            for (a = 0, i = t.length; a < i; a++) x = t[a] * r, u = a < i - 1 ? t[a + 1] * r : e.length, (o = l(e, x, u, r, !1)) === o.next && (o.steiner = !0), s.push(w(o));
            for (s.sort(m), a = 0; a < s.length; a++) C(s[a], n), n = c(n, n.next);
            return n
        }(e, t, f, n)), e.length > 80 * n) {
            r = i = e[0], a = x = e[1];
            for (var v = n; v < h; v += n) (u = e[v]) < r && (r = u), (o = e[v + 1]) < a && (a = o), i < u && (i = u), x < o && (x = o);
            s = 0 !== (s = Math.max(i - r, x - a)) ? 1 / s : 0
        }
        return d(f, y, n, r, a, s), y
    }

    function l(e, t, n, r, a) {
        var i, x;
        if (a === 0 < z(e, t, n, r)) for (i = t; i < n; i += r) x = u(i, e[i], e[i + 1], x); else for (i = n - r; t <= i; i -= r) x = u(i, e[i], e[i + 1], x);
        return x && v(x, x.next) && (A(x), x = x.next), x
    }

    function c(e, t) {
        if (!e) return e;
        t = t || e;
        var n, r = e;
        do {
            if (n = !1, r.steiner || !v(r, r.next) && 0 !== E(r.prev, r, r.next)) r = r.next; else {
                if (A(r), (r = t = r.prev) === r.next) break;
                n = !0
            }
        } while (n || r !== t);
        return t
    }

    function d(e, t, n, r, a, i, x) {
        if (e) {
            !x && i && function (e, t, n, r) {
                var a = e;
                for (; null === a.z && (a.z = g(a.x, a.y, t, n, r)), a.prevZ = a.prev, a.nextZ = a.next, a = a.next, a !== e;) ;
                a.prevZ.nextZ = null, a.prevZ = null, function (e) {
                    var t, n, r, a, i, x, u, o, s = 1;
                    do {
                        for (n = e, i = e = null, x = 0; n;) {
                            for (x++, r = n, t = u = 0; t < s && (u++, r = r.nextZ); t++) ;
                            for (o = s; 0 < u || 0 < o && r;) 0 !== u && (0 === o || !r || n.z <= r.z) ? (n = (a = n).nextZ, u--) : (r = (a = r).nextZ, o--), i ? i.nextZ = a : e = a, a.prevZ = i, i = a;
                            n = r
                        }
                        i.nextZ = null, s *= 2
                    } while (1 < x)
                }(a)
            }(e, r, a, i);
            for (var u, o, s = e; e.prev !== e.next;) if (u = e.prev, o = e.next, i ? h(e, r, a, i) : p(e)) t.push(u.i / n), t.push(e.i / n), t.push(o.i / n), A(e), e = o.next, s = o.next; else if ((e = o) === s) {
                x ? 1 === x ? d(e = f(c(e), t, n), t, n, r, a, i, 2) : 2 === x && y(e, t, n, r, a, i) : d(c(e), t, n, r, a, i, 1);
                break
            }
        }
    }

    function p(e) {
        var t = e.prev, n = e, r = e.next;
        if (!(0 <= E(t, n, r))) {
            for (var a = e.next.next; a !== e.prev;) {
                if (b(t.x, t.y, n.x, n.y, r.x, r.y, a.x, a.y) && 0 <= E(a.prev, a, a.next)) return;
                a = a.next
            }
            return 1
        }
    }

    function h(e, t, n, r) {
        var a = e.prev, i = e, x = e.next;
        if (!(0 <= E(a, i, x))) {
            for (var u = a.x < i.x ? a.x < x.x ? a.x : x.x : i.x < x.x ? i.x : x.x, o = a.y < i.y ? a.y < x.y ? a.y : x.y : i.y < x.y ? i.y : x.y, s = a.x > i.x ? a.x > x.x ? a.x : x.x : i.x > x.x ? i.x : x.x, p = a.y > i.y ? a.y > x.y ? a.y : x.y : i.y > x.y ? i.y : x.y, h = g(u, o, t, n, r), f = g(s, p, t, n, r), y = e.prevZ, v = e.nextZ; y && y.z >= h && v && v.z <= f;) {
                if (y !== e.prev && y !== e.next && b(a.x, a.y, i.x, i.y, x.x, x.y, y.x, y.y) && 0 <= E(y.prev, y, y.next)) return;
                if (y = y.prevZ, v !== e.prev && v !== e.next && b(a.x, a.y, i.x, i.y, x.x, x.y, v.x, v.y) && 0 <= E(v.prev, v, v.next)) return;
                v = v.nextZ
            }
            for (; y && y.z >= h;) {
                if (y !== e.prev && y !== e.next && b(a.x, a.y, i.x, i.y, x.x, x.y, y.x, y.y) && 0 <= E(y.prev, y, y.next)) return;
                y = y.prevZ
            }
            for (; v && v.z <= f;) {
                if (v !== e.prev && v !== e.next && b(a.x, a.y, i.x, i.y, x.x, x.y, v.x, v.y) && 0 <= E(v.prev, v, v.next)) return;
                v = v.nextZ
            }
            return 1
        }
    }

    function f(e, t, n) {
        var r = e;
        do {
            var a = r.prev, i = r.next.next;
            !v(a, i) && M(a, r, r.next, i) && Z(a, i) && Z(i, a) && (t.push(a.i / n), t.push(r.i / n), t.push(i.i / n), A(r), A(r.next), r = e = i), r = r.next
        } while (r !== e);
        return c(r)
    }

    function y(e, t, n, r, a, i) {
        var x, u, o = e;
        do {
            for (var s = o.next.next; s !== o.prev;) {
                if (o.i !== s.i && (u = s, (x = o).next.i !== u.i && x.prev.i !== u.i && !function (e, t) {
                    var n = e;
                    do {
                        if (n.i !== e.i && n.next.i !== e.i && n.i !== t.i && n.next.i !== t.i && M(n, n.next, e, t)) return 1;
                        n = n.next
                    } while (n !== e);
                    return
                }(x, u) && (Z(x, u) && Z(u, x) && function (e, t) {
                    var n = e, r = !1, a = (e.x + t.x) / 2, i = (e.y + t.y) / 2;
                    for (; n.y > i != n.next.y > i && n.next.y !== n.y && a < (n.next.x - n.x) * (i - n.y) / (n.next.y - n.y) + n.x && (r = !r), n = n.next, n !== e;) ;
                    return r
                }(x, u) && (E(x.prev, x, u.prev) || E(x, u.prev, u)) || v(x, u) && 0 < E(x.prev, x, x.next) && 0 < E(u.prev, u, u.next)))) {
                    var p = S(o, s);
                    return o = c(o, o.next), p = c(p, p.next), d(o, t, n, r, a, i), void d(p, t, n, r, a, i)
                }
                s = s.next
            }
            o = o.next
        } while (o !== e)
    }

    function m(e, t) {
        return e.x - t.x
    }

    function C(e, t) {
        if (t = function (e, t) {
            var n, r = t, a = e.x, i = e.y, x = -1 / 0;
            do {
                if (i <= r.y && i >= r.next.y && r.next.y !== r.y) {
                    var u = r.x + (i - r.y) * (r.next.x - r.x) / (r.next.y - r.y);
                    if (u <= a && x < u) {
                        if ((x = u) === a) {
                            if (i === r.y) return r;
                            if (i === r.next.y) return r.next
                        }
                        n = r.x < r.next.x ? r : r.next
                    }
                }
                r = r.next
            } while (r !== t);
            if (!n) return null;
            if (a === x) return n;
            var o, s = n, p = n.x, h = n.y, f = 1 / 0;
            r = n;
            for (; a >= r.x && r.x >= p && a !== r.x && b(i < h ? a : x, i, p, h, i < h ? x : a, i, r.x, r.y) && (o = Math.abs(i - r.y) / (a - r.x), Z(r, e) && (o < f || o === f && (r.x > n.x || r.x === n.x && (v = r, E((y = n).prev, y, v.prev) < 0 && E(v.next, y, y.next) < 0))) && (n = r, f = o)), r = r.next, r !== s;) ;
            var y, v;
            return n
        }(e, t)) {
            var n = S(t, e);
            c(n, n.next)
        }
    }

    function g(e, t, n, r, a) {
        return (e = 1431655765 & ((e = 858993459 & ((e = 252645135 & ((e = 16711935 & ((e = 32767 * (e - n) * a) | e << 8)) | e << 4)) | e << 2)) | e << 1)) | (t = 1431655765 & ((t = 858993459 & ((t = 252645135 & ((t = 16711935 & ((t = 32767 * (t - r) * a) | t << 8)) | t << 4)) | t << 2)) | t << 1)) << 1
    }

    function w(e) {
        for (var t = e, n = e; (t.x < n.x || t.x === n.x && t.y < n.y) && (n = t), (t = t.next) !== e;) ;
        return n
    }

    function b(e, t, n, r, a, i, x, u) {
        return 0 <= (a - x) * (t - u) - (e - x) * (i - u) && 0 <= (e - x) * (r - u) - (n - x) * (t - u) && 0 <= (n - x) * (i - u) - (a - x) * (r - u)
    }

    function E(e, t, n) {
        return (t.y - e.y) * (n.x - t.x) - (t.x - e.x) * (n.y - t.y)
    }

    function v(e, t) {
        return e.x === t.x && e.y === t.y
    }

    function M(e, t, n, r) {
        var a = s(E(e, t, n)), i = s(E(e, t, r)), x = s(E(n, r, e)), u = s(E(n, r, t));
        return a !== i && x !== u || (0 === a && o(e, n, t) || (0 === i && o(e, r, t) || (0 === x && o(n, e, r) || !(0 !== u || !o(n, t, r)))))
    }

    function o(e, t, n) {
        return t.x <= Math.max(e.x, n.x) && t.x >= Math.min(e.x, n.x) && t.y <= Math.max(e.y, n.y) && t.y >= Math.min(e.y, n.y)
    }

    function s(e) {
        return 0 < e ? 1 : e < 0 ? -1 : 0
    }

    function Z(e, t) {
        return E(e.prev, e, e.next) < 0 ? 0 <= E(e, t, e.next) && 0 <= E(e, e.prev, t) : E(e, t, e.prev) < 0 || E(e, e.next, t) < 0
    }

    function S(e, t) {
        var n = new x(e.i, e.x, e.y), r = new x(t.i, t.x, t.y), a = e.next, i = t.prev;
        return (e.next = t).prev = e, (n.next = a).prev = n, (r.next = n).prev = r, (i.next = r).prev = i, r
    }

    function u(e, t, n, r) {
        var a = new x(e, t, n);
        return r ? (a.next = r.next, (a.prev = r).next.prev = a, r.next = a) : (a.prev = a).next = a, a
    }

    function A(e) {
        e.next.prev = e.prev, e.prev.next = e.next, e.prevZ && (e.prevZ.nextZ = e.nextZ), e.nextZ && (e.nextZ.prevZ = e.prevZ)
    }

    function x(e, t, n) {
        this.i = e, this.x = t, this.y = n, this.prev = null, this.next = null, this.z = null, this.prevZ = null, this.nextZ = null, this.steiner = !1
    }

    function z(e, t, n, r) {
        for (var a = 0, i = t, x = n - r; i < n; i += r) a += (e[x] - e[i]) * (e[i + 1] + e[x + 1]), x = i;
        return a
    }

    r.deviation = function (e, t, n, r) {
        var a = t && t.length, i = a ? t[0] * n : e.length, x = Math.abs(z(e, 0, i, n));
        if (a) for (var u = 0, o = t.length; u < o; u++) {
            var s = t[u] * n, p = u < o - 1 ? t[u + 1] * n : e.length;
            x -= Math.abs(z(e, s, p, n))
        }
        var h = 0;
        for (u = 0; u < r.length; u += 3) {
            var f = r[u] * n, y = r[u + 1] * n, v = r[u + 2] * n;
            h += Math.abs((e[f] - e[v]) * (e[1 + y] - e[1 + f]) - (e[f] - e[y]) * (e[1 + v] - e[1 + f]))
        }
        return 0 === x && 0 === h ? 0 : Math.abs((h - x) / x)
    }, r.flatten = function (e) {
        for (var t = e[0][0].length, n = {vertices: [], holes: [], dimensions: t}, r = 0, a = 0; a < e.length; a++) {
            for (var i = 0; i < e[a].length; i++) for (var x = 0; x < t; x++) n.vertices.push(e[a][i][x]);
            0 < a && (r += e[a - 1].length, n.holes.push(r))
        }
        return n
    };
    var a = {
            CLOCKWISE: n.WebGLConstants.CW, COUNTER_CLOCKWISE: n.WebGLConstants.CCW, validate: function (e) {
                return e === a.CLOCKWISE || e === a.COUNTER_CLOCKWISE
            }
        }, i = Object.freeze(a), R = new P.Cartesian3, L = new P.Cartesian3, D = {
            computeArea2D: function (e) {
                for (var t = e.length, n = 0, r = t - 1, a = 0; a < t; r = a++) {
                    var i = e[r], x = e[a];
                    n += i.x * x.y - x.x * i.y
                }
                return .5 * n
            }, computeWindingOrder2D: function (e) {
                return 0 < D.computeArea2D(e) ? i.COUNTER_CLOCKWISE : i.CLOCKWISE
            }, triangulate: function (e, t) {
                return r(P.Cartesian2.packArray(e), t, 2)
            }
        }, U = new P.Cartesian3, _ = new P.Cartesian3, K = new P.Cartesian3, G = new P.Cartesian3, O = new P.Cartesian3,
        V = new P.Cartesian3, k = new P.Cartesian3;
    D.computeSubdivision = function (e, t, n, r) {
        r = T.defaultValue(r, W.CesiumMath.RADIANS_PER_DEGREE);
        var a, i = n.slice(0), x = t.length, u = new Array(3 * x), o = 0;
        for (a = 0; a < x; a++) {
            var s = t[a];
            u[o++] = s.x, u[o++] = s.y, u[o++] = s.z
        }
        for (var p = [], h = {}, f = e.maximumRadius, y = W.CesiumMath.chordLength(r, f), v = y * y; 0 < i.length;) {
            var l, c, d = i.pop(), m = i.pop(), C = i.pop(), g = P.Cartesian3.fromArray(u, 3 * C, U),
                w = P.Cartesian3.fromArray(u, 3 * m, _), b = P.Cartesian3.fromArray(u, 3 * d, K),
                E = P.Cartesian3.multiplyByScalar(P.Cartesian3.normalize(g, G), f, G),
                M = P.Cartesian3.multiplyByScalar(P.Cartesian3.normalize(w, O), f, O),
                Z = P.Cartesian3.multiplyByScalar(P.Cartesian3.normalize(b, V), f, V),
                S = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(E, M, k)),
                A = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(M, Z, k)),
                z = P.Cartesian3.magnitudeSquared(P.Cartesian3.subtract(Z, E, k)), R = Math.max(S, A, z);
            v < R ? S === R ? (a = h[l = Math.min(C, m) + " " + Math.max(C, m)], T.defined(a) || (c = P.Cartesian3.add(g, w, k), P.Cartesian3.multiplyByScalar(c, .5, c), u.push(c.x, c.y, c.z), a = u.length / 3 - 1, h[l] = a), i.push(C, a, d), i.push(a, m, d)) : A === R ? (a = h[l = Math.min(m, d) + " " + Math.max(m, d)], T.defined(a) || (c = P.Cartesian3.add(w, b, k), P.Cartesian3.multiplyByScalar(c, .5, c), u.push(c.x, c.y, c.z), a = u.length / 3 - 1, h[l] = a), i.push(m, a, C), i.push(a, d, C)) : z === R && (a = h[l = Math.min(d, C) + " " + Math.max(d, C)], T.defined(a) || (c = P.Cartesian3.add(b, g, k), P.Cartesian3.multiplyByScalar(c, .5, c), u.push(c.x, c.y, c.z), a = u.length / 3 - 1, h[l] = a), i.push(d, a, m), i.push(a, C, m)) : (p.push(C), p.push(m), p.push(d))
        }
        return new B.Geometry({
            attributes: {
                position: new B.GeometryAttribute({
                    componentDatatype: I.ComponentDatatype.DOUBLE,
                    componentsPerAttribute: 3,
                    values: u
                })
            }, indices: p, primitiveType: B.PrimitiveType.TRIANGLES
        })
    };
    var q = new P.Cartographic, F = new P.Cartographic, j = new P.Cartographic, H = new P.Cartographic;
    D.computeRhumbLineSubdivision = function (e, t, n, r) {
        r = T.defaultValue(r, W.CesiumMath.RADIANS_PER_DEGREE);
        var a, i = n.slice(0), x = t.length, u = new Array(3 * x), o = 0;
        for (a = 0; a < x; a++) {
            var s = t[a];
            u[o++] = s.x, u[o++] = s.y, u[o++] = s.z
        }
        for (var p = [], h = {}, f = e.maximumRadius, y = W.CesiumMath.chordLength(r, f), v = new N.EllipsoidRhumbLine(void 0, void 0, e), l = new N.EllipsoidRhumbLine(void 0, void 0, e), c = new N.EllipsoidRhumbLine(void 0, void 0, e); 0 < i.length;) {
            var d = i.pop(), m = i.pop(), C = i.pop(), g = P.Cartesian3.fromArray(u, 3 * C, U),
                w = P.Cartesian3.fromArray(u, 3 * m, _), b = P.Cartesian3.fromArray(u, 3 * d, K),
                E = e.cartesianToCartographic(g, q), M = e.cartesianToCartographic(w, F),
                Z = e.cartesianToCartographic(b, j);
            v.setEndPoints(E, M);
            var S = v.surfaceDistance;
            l.setEndPoints(M, Z);
            var A = l.surfaceDistance;
            c.setEndPoints(Z, E);
            var z, R, L, D, G = c.surfaceDistance, O = Math.max(S, A, G);
            y < O ? S === O ? (a = h[z = Math.min(C, m) + " " + Math.max(C, m)], T.defined(a) || (R = v.interpolateUsingFraction(.5, H), L = .5 * (E.height + M.height), D = P.Cartesian3.fromRadians(R.longitude, R.latitude, L, e, k), u.push(D.x, D.y, D.z), a = u.length / 3 - 1, h[z] = a), i.push(C, a, d), i.push(a, m, d)) : A === O ? (a = h[z = Math.min(m, d) + " " + Math.max(m, d)], T.defined(a) || (R = l.interpolateUsingFraction(.5, H), L = .5 * (M.height + Z.height), D = P.Cartesian3.fromRadians(R.longitude, R.latitude, L, e, k), u.push(D.x, D.y, D.z), a = u.length / 3 - 1, h[z] = a), i.push(m, a, C), i.push(a, d, C)) : G === O && (a = h[z = Math.min(d, C) + " " + Math.max(d, C)], T.defined(a) || (R = c.interpolateUsingFraction(.5, H), L = .5 * (Z.height + E.height), D = P.Cartesian3.fromRadians(R.longitude, R.latitude, L, e, k), u.push(D.x, D.y, D.z), a = u.length / 3 - 1, h[z] = a), i.push(d, a, m), i.push(a, C, m)) : (p.push(C), p.push(m), p.push(d))
        }
        return new B.Geometry({
            attributes: {
                position: new B.GeometryAttribute({
                    componentDatatype: I.ComponentDatatype.DOUBLE,
                    componentsPerAttribute: 3,
                    values: u
                })
            }, indices: p, primitiveType: B.PrimitiveType.TRIANGLES
        })
    }, D.scaleToGeodeticHeight = function (e, t, n, r) {
        n = T.defaultValue(n, P.Ellipsoid.WGS84);
        var a = R, i = L;
        if (t = T.defaultValue(t, 0), r = T.defaultValue(r, !0), T.defined(e)) for (var x = e.length, u = 0; u < x; u += 3) P.Cartesian3.fromArray(e, u, i), r && (i = n.scaleToGeodeticSurface(i, i)), 0 !== t && (a = n.geodeticSurfaceNormal(i, a), P.Cartesian3.multiplyByScalar(a, t, a), P.Cartesian3.add(i, a, i)), e[u] = i.x, e[u + 1] = i.y, e[u + 2] = i.z;
        return e
    }, e.PolygonPipeline = D, e.WindingOrder = i
});
