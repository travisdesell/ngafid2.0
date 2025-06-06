define(["exports", "./when-c2e8ef35"], function (e, n) {
    "use strict";

    function r(e) {
        var t;
        this.name = "DeveloperError", this.message = e;
        try {
            throw new Error
        } catch (e) {
            t = e.stack
        }
        this.stack = t
    }

    n.defined(Object.create) && ((r.prototype = Object.create(Error.prototype)).constructor = r), r.prototype.toString = function () {
        var e = this.name + ": " + this.message;
        return n.defined(this.stack) && (e += "\n" + this.stack.toString()), e
    }, r.throwInstantiationError = function () {
        throw new r("This function defines an interface and should not be called directly.")
    };
    var f = {};

    function o(e, t, n) {
        return "Expected " + n + " to be typeof " + t + ", actual typeof was " + e
    }

    f.typeOf = {}, f.defined = function (e, t) {
        if (!n.defined(t)) throw new r(e + " is required, actual value was undefined")
    }, f.typeOf.func = function (e, t) {
        if ("function" != typeof t) throw new r(o(typeof t, "function", e))
    }, f.typeOf.string = function (e, t) {
        if ("string" != typeof t) throw new r(o(typeof t, "string", e))
    }, f.typeOf.number = function (e, t) {
        if ("number" != typeof t) throw new r(o(typeof t, "number", e))
    }, f.typeOf.number.lessThan = function (e, t, n) {
        if (f.typeOf.number(e, t), n <= t) throw new r("Expected " + e + " to be less than " + n + ", actual value was " + t)
    }, f.typeOf.number.lessThanOrEquals = function (e, t, n) {
        if (f.typeOf.number(e, t), n < t) throw new r("Expected " + e + " to be less than or equal to " + n + ", actual value was " + t)
    }, f.typeOf.number.greaterThan = function (e, t, n) {
        if (f.typeOf.number(e, t), t <= n) throw new r("Expected " + e + " to be greater than " + n + ", actual value was " + t)
    }, f.typeOf.number.greaterThanOrEquals = function (e, t, n) {
        if (f.typeOf.number(e, t), t < n) throw new r("Expected " + e + " to be greater than or equal to" + n + ", actual value was " + t)
    }, f.typeOf.object = function (e, t) {
        if ("object" != typeof t) throw new r(o(typeof t, "object", e))
    }, f.typeOf.bool = function (e, t) {
        if ("boolean" != typeof t) throw new r(o(typeof t, "boolean", e))
    }, f.typeOf.number.equals = function (e, t, n, o) {
        if (f.typeOf.number(e, n), f.typeOf.number(t, o), n !== o) throw new r(e + " must be equal to " + t + ", the actual values are " + n + " and " + o)
    }, e.Check = f, e.DeveloperError = r
});
