-- ------------------------------------------------------------------------------------
-- 运行(WINDOWS)ghci命令行运行方式
-- WIN + R 启动 CMD
-- cd F:\slicef\ws\gitws\malonylcoa\src\main\hs\hws\cases # 进入程序文件根目录
-- 启动 ghci
-- :l exception-case.hs -- 加载程序文件
-- main -- 启动main程序
-- 测试正常情况：
-- 结果: 3
-- 测试异常情况：
-- 发生异常: ZeroValue
-- --------------------------------------------------------------------------------------

-- 导入必要的模块
import Control.Exception
import Data.Typeable

-- 定义自定义异常类型: 0值异常
-- 在异常处理中，Typeable 起着关键作用。当定义自定义异常类型时，
-- 通常需要让该类型成为 Typeable 的实例，以便在异常捕获和处理时能够进行类型检查
-- 这使得 try 函数能够正确地捕获和区分不同类型的异常。
data ZeroValue = ZeroValue deriving (Show, Typeable)

-- 使自定义异常类型成为 Exception 类型类的实例
instance Exception ZeroValue

-- 定义自定义运算函数：若计算结果为0抛出异常 ZeroValue
myop :: Int -> Int -> IO Int
myop x y
    | x + y == 0 = throwIO ZeroValue
    | otherwise = return (x + y)

-- 主函数，用于测试异常处理
main :: IO ()
main = do
    putStrLn "测试正常情况："
    result1 <- try (myop 1 2) :: IO (Either ZeroValue Int)
    case result1 of
        Left err -> putStrLn $ "发生异常: " ++ show err
        Right val -> putStrLn $ "结果: " ++ show val

    putStrLn "测试异常情况："
    result2 <- try (myop (-1) 1) :: IO (Either ZeroValue Int)
    case result2 of
        Left err -> putStrLn $ "发生异常: " ++ show err
        Right val -> putStrLn $ "结果: " ++ show val
