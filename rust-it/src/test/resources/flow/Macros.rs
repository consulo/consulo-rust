macro_rules! my_return {
    ($e:expr) => { return $e };
}

macro_rules! twice {
    ($e:expr) => { $e + $e };
}

fn macro_return() -> i32 {
    my_return!(1);
    2
}

fn macro_expr(a: i32) -> i32 {
    twice!(a)
}
