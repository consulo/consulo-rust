// `const` traits and impls, and `[const]` bounds
pub const trait Clone: Sized {
    fn clone(&self) -> Self;
}

pub(crate) const unsafe trait BytewiseEq<Rhs = Self>: [const] PartialEq<Rhs> + Sized {}

const impl<T, A: [const] Allocator + [const] Destruct> RawVec<T, A> {}

impl<T: [const] Default> const Default for Cell<T> {}

pub const fn take<T: [const] Default>(dest: &mut T) -> T where T: [const] Destruct {
    loop {}
}

pub const fn is_some_and(self, f: impl [const] FnOnce(T) -> bool + [const] Destruct) -> bool {
    false
}

// precise capturing
fn iter<'a, T>(v: &'a [T]) -> impl Iterator<Item = &'a T> + use<'a, T> {
    v.iter()
}

// `safe` items of an `unsafe extern` block
unsafe extern "C" {
    pub safe fn sqrt(x: f64) -> f64;
    pub(crate) safe fn cbrt(x: f64) -> f64;
    safe static ERRNO: i32;
    pub unsafe fn free(p: *mut u8);
}

// a C-variadic function pointer type with a named `...`
type Callback = unsafe extern "C" fn(_: *mut u8, _: ...) -> i32;

// `safe` is still an ordinary identifier
fn safe() -> i32 {
    let safe = 1;
    safe
}
