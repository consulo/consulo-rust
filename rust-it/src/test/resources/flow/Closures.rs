fn closure() -> i32 {
    let a = 1;
    let f = |x: i32| x + a;
    f(2)
}

fn closure_return() {
    let f = || {
        return 1;
    };
    let _ = f;
}

fn nested() -> i32 {
    fn inner(a: i32) -> i32 {
        a + 1
    }
    inner(1)
}

struct S { a: i32 }

impl S {
    fn method(&self) -> i32 {
        self.a
    }
}

fn struct_literal(a: i32) -> S {
    S { a }
}

fn async_block() {
    let _f = async { 1 };
}
