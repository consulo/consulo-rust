fn empty() {}

fn straight(a: i32) -> i32 {
    let x = 1;
    let y = x + a;
    y * 2
}

fn assign() {
    let mut x;
    x = 1;
    x += 2;
    let (a, b) = (x, 3);
    let _ = [a, b];
}

fn tail_unit() {
    let x = 1;
    x;
}
