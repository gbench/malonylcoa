#include <stdio.h>
#include <malloc.h>
#include <string.h>
#include <time.h>

/**
 * 矩阵数据结构
 */
typedef struct
{
    double *data;
    int n;
    int mod;
} ndarray;

/**
 *
 * @param n 数据长度
 * @param mod 取模长度
 * @param flag 是否初始化
 */
ndarray nd_create(const int n, const int mod, const char flag)
{
    const ndarray a = {.data = (double *)malloc(sizeof(double) * n), .n = n, .mod = mod};

    if (flag)
    {
        for (int i = 0; i < n; i++)
        {
            a.data[i] = i * 1.0;
        }
    }
    else
    {
        memset(a.data, 0, sizeof(double) * n);
    }
    return a;
}

/**
 * @param a
 * @param b
 */
ndarray mmult(const ndarray a, const ndarray b)
{
    const int mod = a.mod;
    const int nrow = a.n / a.mod;
    const int ncol = b.mod;
    const int mxn = nrow * ncol;
    const ndarray c = nd_create(mxn, ncol, 0);

    for (int i = 0; i < nrow; i++)
    {
        for (int j = 0; j < ncol; j++)
        {
            double d = 0;
            for (int k = 0; k < mod; k++)
            {
                d += a.data[i * mod + k] * b.data[k * ncol + j];
            }
            c.data[i * ncol + j] = d;
        }
    }

    return c;
}

/**
 * 矩阵转置
 */
ndarray transpose(const ndarray nd)
{
    const int tmod = nd.n / nd.mod;
    const ndarray tnd = nd_create(nd.n, tmod, 0);

    for (int i = 0; i < tnd.n; i++)
    {
        tnd.data[i] = nd.data[i % tmod * nd.mod + i / tmod];
    }

    return tnd;
}

/**
 *
 */
void print(const ndarray nd)
{
    for (int i = 0; i < nd.n; i++)
    {
        printf("%.2f\t%c", nd.data[i], (i + 1) % nd.mod == 0 ? '\n' : ' ');
    }
}

/**
 *
 */
void destroy(const ndarray nd)
{
    free(nd.data);
}

/**
 *
 */
int main()
{
    for (int i = 0; i < 100; i++)
    {
        const int n = (i + 1) * 10;
        time_t start_time;
        time_t end_time;

        time(&start_time);
        const ndarray nd = nd_create(n * n, n, 1);
        // printf("-------------------\n");
        // print(nd);
        // const ndarray nd1 = transpose(nd);
        // printf("-------------------\n");
        // print(nd1);
        const ndarray nx = mmult(nd, nd);
        // printf("%d,%f\n", nx.n, *nx.data);
        //  print(nx);
        time(&end_time);
        const long lasts = difftime(end_time, start_time);
        printf("\n%d--->lasts for:%d,data[0]:%.2f\n", n, lasts, nx.data[0]);
        destroy(nd);
        // destroy(nd1);
        destroy(nx);
    }

    return 0;
}