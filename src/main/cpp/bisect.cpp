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
 * #ref https://blog.csdn.net/witton/article/details/145187279
 * 
 ********************************************************/

#include <stdio.h>
#include <float.h>
#include <math.h>
#include <functional>
#include <type_traits>
#include <iostream>
#include <vector>
#include <string>

// ----------------------------------------------------------------------------------
// 类型转换工具
// ----------------------------------------------------------------------------------

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

// 辅助模板类，用于判断所有类型是否都能转换为 T
template <typename T, typename... Args>
struct all_convertible;

template <typename T>
struct all_convertible<T> : std::true_type {};

template <typename T, typename First, typename... Rest>
struct all_convertible<T, First, Rest...>
    : std::integral_constant<bool, std::is_convertible<First, T>::value && all_convertible<T, Rest...>::value> {};

// ----------------------------------------------------------------------------------
// 数据结构
// ----------------------------------------------------------------------------------

// 前置声明 vec 类，以便在 is_vec 中使用
template<typename T>
struct vec;

// 辅助模板来判断类型是否为 vec
template<typename T>
struct is_vec : std::false_type {};

template<typename T>
struct is_vec<vec<T>> : std::true_type {};

// 定义模板结构体 vec
template <typename T>
struct vec {
    // 构造函数，使用可变参数模板
    template <typename... Args, typename std::enable_if<all_convertible<T, Args...>::value, int>::type = 0>
    vec(Args&&... args) : size(sizeof...(args)), data(new T[size]) {
        initialize(0, std::forward<Args>(args)...);
    }

    // 根据 lambda 函数生成向量的构造函数
    vec(size_t s, std::function<T(size_t)> generator) : size(s) {
        data = new T[size];
        for (size_t i = 0; i < size; ++i) {
            data[i] = generator(i);
        }
    }

    // 拷贝构造函数
    vec(const vec& other) : size(other.size), data(new T[size]) {
        for (std::size_t i = 0; i < size; ++i) {
            data[i] = other.data[i];
        }
    }

    // 移动构造函数
    vec(vec&& other) noexcept : size(other.size), data(other.data) {
        other.size = 0;
        other.data = nullptr;
    }

    // 拷贝赋值运算符
    vec& operator=(const vec& other) {
        if (this != &other) {
            delete[] data;
            size = other.size;
            data = new T[size];
            for (std::size_t i = 0; i < size; ++i) {
                data[i] = other.data[i];
            }
        }
        return *this;
    }

    // 移动赋值运算符
    vec& operator=(vec&& other) noexcept {
        if (this != &other) {
            delete[] data;
            size = other.size;
            data = other.data;
            other.size = 0;
            other.data = nullptr;
        }
        return *this;
    }

    // 析构函数，释放动态分配的内存
    ~vec() {
        delete[] data;
    }

    // 辅助函数，用于递归初始化数组
    template <typename First, typename... Rest>
    void initialize(std::size_t index, First&& first, Rest&&... rest) {
        data[index] = static_cast<T>(std::forward<First>(first));
        initialize(index + 1, std::forward<Rest>(rest)...);
    }

    void initialize(std::size_t) {}

    // [] 索引访问元素
    T& operator[](size_t index) {
        return data[index];
    }

    const T& operator[](size_t index) const {
        return data[index];
    }

    // 对 vec 每个元素 t 应用 mapper(t) 变换成 vec<U>
    template<typename U>
    vec<U> fmap(std::function<U(const T&)> mapper) const {
        return vec<U>(size, [this, mapper](size_t i) {
            return mapper(this->data[i]);
        });
    }

    // 重载 fmap 函数，支持 U (int, T&) 类型的 mapper
    template<typename U>
    vec<U> fmap(std::function<U(int, T&)> mapper) const {
        return vec<U>(size, [this, mapper](size_t i) {
            return mapper(static_cast<int>(i), this->data[i]);
        });
    }

    // vec<U> 与单个元素 U 相乘
    vec<T> operator*(const T& u) const {
        return vec<T>(size, [this, u](size_t i) {
            return this->data[i] * u;
        });
    }

    // vec<U> 与另一个 vec<U> 按照对应位置相乘
    vec<T> operator*(const vec<T>& us) const {
        if (size != us.size) {
            throw std::invalid_argument("Vectors must have the same size for element-wise multiplication.");
        }
        return vec<T>(size, [this, &us](size_t i) {
            return this->data[i] * us.data[i];
        });
    }

    // vec<U> 与单个元素 U 相加
    vec<T> operator+(const T& u) const {
        return vec<T>(size, [this, u](size_t i) {
            return this->data[i] + u;
        });
    }

    // vec<U> 与另一个 vec<U> 按照对应位置相加
    vec<T> operator+(const vec<T>& us) const {
        if (size != us.size) {
            throw std::invalid_argument("Vectors must have the same size for element-wise addition.");
        }
        return vec<T>(size, [this, &us](size_t i) {
            return this->data[i] + us.data[i];
        });
    }

    // vec<U> 与单个元素 U 相减
    vec<T> operator-(const T& u) const {
        return vec<T>(size, [this, u](size_t i) {
            return this->data[i] - u;
        });
    }

    // vec<U> 与另一个 vec<U> 按照对应位置相减
    vec<T> operator-(const vec<T>& us) const {
        if (size != us.size) {
            throw std::invalid_argument("Vectors must have the same size for element-wise subtraction.");
        }
        return vec<T>(size, [this, &us](size_t i) {
            return this->data[i] - us.data[i];
        });
    }

    // vec<U> 与单个元素 U 相除
    vec<T> operator/(const T& u) const {
        if (u == T()) {
            throw std::invalid_argument("Division by zero is not allowed.");
        }
        return vec<T>(size, [this, u](size_t i) {
            return this->data[i] / u;
        });
    }

    // vec<U> 与另一个 vec<U> 按照对应位置相除
    vec<T> operator/(const vec<T>& us) const {
        if (size != us.size) {
            throw std::invalid_argument("Vectors must have the same size for element-wise division.");
        }
        for (size_t i = 0; i < size; ++i) {
            if (us.data[i] == T()) {
                throw std::invalid_argument("Division by zero is not allowed.");
            }
        }
        return vec<T>(size, [this, &us](size_t i) {
            return this->data[i] / us.data[i];
        });
    }

    // 对 vec 中各个元素求和
    T sum() const {
        T result = T();
        for (size_t i = 0; i < size; ++i) {
            result += data[i];
        }
        return result;
    }

    // 将数组内容转换为字符串的方法
    // 基础类型的 to_string 实现
    template<typename U = T>
    typename std::enable_if<std::is_fundamental<U>::value, std::string>::type
    to_string() const {
        std::string result = "[";
        for (std::size_t i = 0; i < size; ++i) {
            result += std::to_string(data[i]);
            if (i < size - 1) {
                result += ", ";
            }
        }
        result += "]";
        return result;
    }

    // vec 类型的 to_string 实现
    template<typename U = T>
    typename std::enable_if<is_vec<U>::value, std::string>::type
    to_string() const {
        std::string result = "[";
        for (std::size_t i = 0; i < size; ++i) {
            result += data[i].to_string();
            if (i < size - 1) {
                result += ", ";
            }
        }
        result += "]";
        return result;
    }

    // 数据成员
    std::size_t size;
    T* data;
};

template <typename T>
std::ostream& operator << (std::ostream &os, vec<T> xs) {
	os << xs.to_string();
	return os;
};

// ----------------------------------------------------------------------------------
// 算法正文
// ----------------------------------------------------------------------------------

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

template <typename T>
T npv (T rate, vec<T> pmts, T price) {
    // 显式创建 std::function 对象
    std::function<T(int, T&)> mapper = [=](int i, T& p) {
        return p * std::pow(1 + rate, -(i + 1));
    };
    return pmts.fmap(mapper).sum() + price;
}

int main() {
	std::cout << "* SQRT OF [0..9]" << std::endl;
	for (int i=0; i<10; i++) {
  		double v = bisect(to_f([&i](double x) {return pow(x, 2) - i;}), 0, 10);
		printf("sqrt(%d) = %.3f \n", i, v);
	}

	printf("-----------------------------------------------------------------------------\n");

	double price = 10000; // 1万元现金资产
	auto n = 12; // 一年的月份数量
	auto m = 30; // 一月的天数
	auto fee = .5; // 日费用
	auto pmts = vec<double>(n, [=](auto x){return (price+m*n*fee)/n;}); // 购买1万元现金资产，以日利息0.5元的融资费用，产生的月度支付序列
	auto rate = bisect(to_f([&](double r){return npv(r, pmts*-1, price);})); // 内部收益率(折现利率）
	auto pvs = pmts.fmap<double>([=](int i, auto &x){ return x*pow(1+rate, -(i+1));}); // 现值序列
	auto interests = pmts-pvs; // 利息

	std::cout << "* TOTAL_PVS: " << pvs.sum() << std::endl;
	std::cout << "* TOTAL_INTERESTS: " << interests.sum() << std::endl;
	std::cout << "* IRR: " << rate << std::endl;
	std::cout << "* PVS: " << pvs << std::endl;
	std::cout << "* PMTS: " << pmts << std::endl;
	std::cout << "* INTERESTS: " << interests << std::endl;
	
	return 0;
}
