/*********************************************************
 *
 * F:\slicef\ws\gitws\malonylcoa\src\main\cpp>g++ --version
 * g++ (MinGW.org GCC-6.3.0-1) 6.3.0
 * Copyright (C) 2016 Free Software Foundation, Inc.
 * This is free software; see the source for copying conditions.  There is NO
 * warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * g++ bisect.cpp && a
 * 
 ********************************************************/

#include <stdio.h>
#include <float.h>
#include <math.h>
#include <functional>
#include <type_traits>

// 实现函数，用于将 lambda 表达式转换为普通函数
template <class L, class R, class... Args>
static auto impl_impl(L&& l) {
    static_assert(
        !std::is_same<L, std::function<R(Args...)>>::value,
        "Only lambdas are supported, it is unsafe to use std::function or other non - lambda callables"
    );
    // 这里将 lambda 表达式类对象保存下来，设置为静态变量，
    // 后面的新的 lambda 表达式才能访问到
    static L lambda_s = std::move(l);
    // 返回一个新的 lambda 表达式
    return [](Args... args) -> R {
        // 调用保存的 lambda 表达式类对象的 `operator()` 方法
        return lambda_s(args...);
    };
}

// 辅助模板类，用于推导 lambda 表达式的调用类型
template <class L>
struct to_f_impl : public to_f_impl<decltype(&L::operator())> {
    // 这里先定义一个模板，只是确定模板参数用，方便后面的模板特化
    // 模板参数即为：decltype(&L::operator())，
    // 即根据 L 的 `operator()` 方法来确定，
    // 而一个类的 `operator()` 方法格式为：
    // ReturnType ClassType::operator()(ArgType1, ArgType2, ... ArgTypeN)
    // 使用模板来表示则为：
    // template <typename ReturnType, typename ClassType, typename... ArgTypes>
};

// 特化模板类，处理 const 修饰的 lambda 表达式的 operator() 方法
template <class ClassType, class R, class... Args>
struct to_f_impl<R(ClassType::*)(Args...) const> {
    // 这里还需要带上 lambda 表达式类的类型 L，方便传递 lambda 表达式类对象
    template <class L>
    static auto impl(L&& l) {
        return impl_impl<L, R, Args...>(std::move(l));
    }
};

// 特化模板类，处理非 const 修饰的 lambda 表达式的 operator() 方法
template <class ClassType, class R, class... Args>
struct to_f_impl<R(ClassType::*)(Args...)> {
    // 这里还需要带上 lambda 表达式类的类型 L，方便传递 lambda 表达式类对象
    template <class L>
    static auto impl(L&& l) {
        return impl_impl<L, R, Args...>(std::move(l));
    }
};

// 最终的封装 API，用于将 lambda 表达式转换为普通函数
template <class L>
static auto to_f(L&& l) {
    return to_f_impl<L>::impl(std::move(l));
}

typedef double (*fn)(double);

double bisect(fn f, double a=0, double b=1, double eps=1e-10) {
	double fa = f(a), fb = f(b);
	if (fa * fb > 0) return NAN; 
	else if (fabs(fa) < eps) return a;
	else if (fabs(fb) < eps) return b;
	else {
loop: 		double c = (a + b) / 2, fc = f(c);
		if (fabs(fc) < eps || fabs((b-a)/2) < eps) return c;
		else {
			double flag = fa * fc > 0;
			*(flag > 0 ? &a : &b) = c;
			if (flag > 0) fa = fc;
			goto loop;
		}
	}
}

int main() {
	for (int i=0;i<10;i++) {
  		double v = bisect(to_f([&i](double x) {return pow(x, 2) - i;}), 0, 10);
		printf("sqrt(%d) = %.8f \n", i, v);
	}
	return 0;
}
