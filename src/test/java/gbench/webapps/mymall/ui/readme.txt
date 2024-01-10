#设置仓库
npm config set registry https://registry.npm.taobao.org

#查看配置
npm config list

# 安装 webpack 
npm install webpack webpack-dev-server webpack-cli  -g
npm install webpack webpack-dev-server webpack-cli  --save-dev webpack
npm install babel-core babel-loader babel-preset-env --save-dev

# 安装依赖包
npm install vue@latest vuex@latest vue-router@latest --save-dev

# 安装依赖包
npm install eslint-webpack-plugin@latest html-webpack-plugin@latest --save-dev
npm install css-loader@latest style-loader@latest --save-dev
npm install @babel/eslint-parser --save-dev
npm install @babel/preset-env --save-dev

# 安装依赖包
npm install axios

#编译前端文件
webpack-dev-server --config local-webpack.config.js