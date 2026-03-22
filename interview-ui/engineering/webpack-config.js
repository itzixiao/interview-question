/**
 * Webpack 配置详解与优化
 *
 * 核心配置模块：
 * 1. 基础配置 - entry、output、mode
 * 2. 模块处理 - loader 配置
 * 3. 插件配置 - plugins
 * 4. 开发配置 - devServer
 * 5. 优化配置 - optimization
 * 6. 性能优化策略
 *
 * 对应文档：docs/18-前端开发/03-前端工程化详解.md
 */

const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const TerserPlugin = require('terser-webpack-plugin');
const {BundleAnalyzerPlugin} = require('webpack-bundle-analyzer');
const webpack = require('webpack');

/**
 * 判断当前环境
 */
const isProduction = process.env.NODE_ENV === 'production';
const isDevelopment = !isProduction;

/**
 * Webpack 配置对象
 */
const config = {
    // ==================== 1. 基础配置 ====================

    /**
     * 模式（mode）
     * - development: 开发模式，启用开发优化（如更好的错误信息）
     * - production: 生产模式，启用代码压缩、优化等
     */
    mode: isProduction ? 'production' : 'development',

    /**
     * 入口（entry）
     * 指定 Webpack 从哪个文件开始构建依赖图
     */
    entry: {
        // 单入口
        main: './src/index.js',

        // 多入口（代码分割场景）
        vendor: './src/vendor.js',

        // 动态入口（根据条件）
        // entry: isProduction ? './src/index.prod.js' : './src/index.dev.js'
    },

    /**
     * 输出（output）
     * 配置打包后的文件输出位置和命名
     */
    output: {
        // 输出目录（绝对路径）
        path: path.resolve(__dirname, 'dist'),

        // 输出文件名
        filename: isProduction
            ? 'js/[name].[contenthash:8].js'  // 生产环境带 hash
            : 'js/[name].js',                   // 开发环境不带 hash

        //  chunk 文件名（代码分割产生的文件）
        chunkFilename: 'js/[name].[contenthash:8].chunk.js',

        // 静态资源输出目录
        assetModuleFilename: 'assets/[name].[contenthash:8][ext]',

        // 公共路径（CDN 配置）
        publicPath: isProduction ? 'https://cdn.example.com/' : '/',

        // 清理输出目录
        clean: true,

        // 库打包配置（用于发布 npm 包）
        library: {
            name: 'MyLibrary',
            type: 'umd'
        },

        // 全局对象（Node.js 环境）
        globalObject: 'this'
    },

    // ==================== 2. 模块处理（Module）====================

    module: {
        /**
         * 规则数组，每个规则定义如何处理特定类型的文件
         */
        rules: [
            // -------------------- JavaScript/TypeScript --------------------
            {
                test: /\.(js|jsx|ts|tsx)$/,  // 匹配文件类型
                exclude: /node_modules/,      // 排除 node_modules
                include: path.resolve(__dirname, 'src'),  // 只处理 src 目录

                use: [
                    {
                        loader: 'babel-loader',
                        options: {
                            presets: [
                                // 预设：根据目标浏览器自动转换语法
                                ['@babel/preset-env', {
                                    targets: {
                                        browsers: ['> 1%', 'last 2 versions', 'not dead']
                                    },
                                    useBuiltIns: 'usage',  // 按需引入 polyfill
                                    corejs: 3
                                }],
                                '@babel/preset-react',     // React 支持
                                '@babel/preset-typescript' // TypeScript 支持
                            ],
                            plugins: [
                                '@babel/plugin-proposal-class-properties',    // 类属性
                                '@babel/plugin-proposal-decorators',          // 装饰器
                                '@babel/plugin-syntax-dynamic-import'         // 动态导入
                            ],
                            cacheDirectory: true  // 启用缓存
                        }
                    }
                ]
            },

            // -------------------- CSS --------------------
            {
                test: /\.css$/,
                use: [
                    // 生产环境提取 CSS，开发环境使用 style-loader
                    isProduction ? MiniCssExtractPlugin.loader : 'style-loader',
                    {
                        loader: 'css-loader',
                        options: {
                            importLoaders: 1,  // 使用 postcss-loader 处理 @import
                            modules: {          // CSS Modules 配置
                                auto: true,     // 自动识别 .module.css 文件
                                localIdentName: isProduction
                                    ? '[hash:base64:8]'
                                    : '[path][name]__[local]--[hash:base64:5]'
                            }
                        }
                    },
                    'postcss-loader'  // 处理 CSS（添加浏览器前缀等）
                ]
            },

            // -------------------- SCSS/SASS --------------------
            {
                test: /\.s[ac]ss$/i,
                use: [
                    isProduction ? MiniCssExtractPlugin.loader : 'style-loader',
                    'css-loader',
                    'postcss-loader',
                    {
                        loader: 'sass-loader',
                        options: {
                            additionalData: `@use "@/styles/vars.scss" as *;`,  // 全局变量
                            sassOptions: {
                                includePaths: [path.resolve(__dirname, 'src/styles')]
                            }
                        }
                    }
                ]
            },

            // -------------------- 图片资源 --------------------
            {
                test: /\.(png|jpe?g|gif|svg|webp)$/i,
                type: 'asset',  // Webpack 5 新特性，替代 url-loader/file-loader
                parser: {
                    dataUrlCondition: {
                        maxSize: 8 * 1024  // 8KB 以下的图片转为 base64
                    }
                },
                generator: {
                    filename: 'images/[name].[contenthash:8][ext]'
                }
            },

            // -------------------- 字体文件 --------------------
            {
                test: /\.(woff2?|eot|ttf|otf)$/i,
                type: 'asset/resource',
                generator: {
                    filename: 'fonts/[name].[contenthash:8][ext]'
                }
            },

            // -------------------- 其他资源 --------------------
            {
                test: /\.(mp4|webm|ogg|mp3|wav|flac|aac)$/i,
                type: 'asset/resource',
                generator: {
                    filename: 'media/[name].[contenthash:8][ext]'
                }
            }
        ]
    },

    // ==================== 3. 解析配置（Resolve）====================

    resolve: {
        /**
         * 别名配置，简化导入路径
         */
        alias: {
            '@': path.resolve(__dirname, 'src'),
            '@components': path.resolve(__dirname, 'src/components'),
            '@utils': path.resolve(__dirname, 'src/utils'),
            '@assets': path.resolve(__dirname, 'src/assets'),
            '@styles': path.resolve(__dirname, 'src/styles')
        },

        /**
         * 自动解析的文件扩展名
         */
        extensions: ['.js', '.jsx', '.ts', '.tsx', '.json', '.vue'],

        /**
         * 模块查找目录
         */
        modules: ['node_modules', path.resolve(__dirname, 'src')]
    },

    // ==================== 4. 插件（Plugins）====================

    plugins: [
        /**
         * HTML 模板插件
         * 自动生成 HTML 文件并注入打包后的资源
         */
        new HtmlWebpackPlugin({
            template: './public/index.html',  // 模板文件
            filename: 'index.html',           // 输出文件名
            title: 'My App',
            favicon: './public/favicon.ico',
            minify: isProduction ? {
                removeComments: true,         // 移除注释
                collapseWhitespace: true,     // 压缩空格
                removeRedundantAttributes: true,  // 移除冗余属性
                useShortDoctype: true
            } : false,
            // 控制 chunk 注入
            chunks: ['main', 'vendor']
        }),

        /**
         * CSS 提取插件（生产环境）
         * 将 CSS 提取到单独的文件中
         */
        ...(isProduction ? [
            new MiniCssExtractPlugin({
                filename: 'css/[name].[contenthash:8].css',
                chunkFilename: 'css/[name].[contenthash:8].chunk.css'
            })
        ] : []),

        /**
         * 定义全局常量
         */
        new webpack.DefinePlugin({
            'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV),
            'process.env.API_BASE_URL': JSON.stringify(
                isProduction ? 'https://api.example.com' : 'http://localhost:3000'
            )
        }),

        /**
         * 进度插件
         */
        new webpack.ProgressPlugin(),

        /**
         * 包分析插件（仅在 ANALYZE 环境变量存在时启用）
         */
        ...(process.env.ANALYZE ? [
            new BundleAnalyzerPlugin({
                analyzerMode: 'server',
                openAnalyzer: true
            })
        ] : [])
    ],

    // ==================== 5. 开发服务器（DevServer）====================

    devServer: {
        /**
         * 静态文件服务
         */
        static: {
            directory: path.join(__dirname, 'public')
        },

        /**
         * 启用热模块替换（HMR）
         */
        hot: true,

        /**
         * 自动打开浏览器
         */
        open: true,

        /**
         * 端口号
         */
        port: 3000,

        /**
         * 启用 gzip 压缩
         */
        compress: true,

        /**
         * 历史路由支持（SPA）
         */
        historyApiFallback: true,

        /**
         * 代理配置
         */
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                pathRewrite: {
                    '^/api': ''
                }
            }
        },

        /**
         * 启用 HTTPS
         */
        // https: true,

        /**
         * 客户端日志级别
         */
        client: {
            logging: 'info',
            overlay: {
                errors: true,
                warnings: false
            }
        }
    },

    // ==================== 6. 优化配置（Optimization）====================

    optimization: {
        /**
         * 代码压缩
         */
        minimize: isProduction,
        minimizer: [
            // JS 压缩
            new TerserPlugin({
                parallel: true,           // 并行压缩
                terserOptions: {
                    compress: {
                        drop_console: true,      // 移除 console
                        drop_debugger: true,     // 移除 debugger
                        pure_funcs: ['console.log']  // 移除指定函数
                    },
                    format: {
                        comments: false          // 移除注释
                    }
                },
                extractComments: false    // 不提取注释到单独文件
            }),

            // CSS 压缩
            new CssMinimizerPlugin()
        ],

        /**
         * 代码分割
         */
        splitChunks: {
            chunks: 'all',  // 对所有 chunk 进行分割

            // 缓存组配置
            cacheGroups: {
                // 提取第三方库
                vendor: {
                    name: 'vendors',
                    test: /[\\/]node_modules[\\/]/,
                    priority: 10,  // 优先级
                    chunks: 'all'
                },

                // 提取公共代码
                common: {
                    name: 'common',
                    minChunks: 2,  // 至少被 2 个 chunk 引用才提取
                    priority: 5,
                    reuseExistingChunk: true  // 复用已有 chunk
                },

                // 提取样式
                styles: {
                    name: 'styles',
                    test: /\.(css|scss|sass)$/,
                    chunks: 'all',
                    enforce: true
                }
            }
        },

        /**
         * 运行时 chunk
         * 将运行时代码提取到单独文件
         */
        runtimeChunk: {
            name: 'runtime'
        },

        /**
         * 模块标识符优化
         */
        moduleIds: isProduction ? 'deterministic' : 'named',
        chunkIds: isProduction ? 'deterministic' : 'named'
    },

    // ==================== 7. 性能提示 ====================

    performance: {
        // 性能提示开关
        hints: isProduction ? 'warning' : false,

        // 入口文件大小限制（字节）
        maxEntrypointSize: 250000,

        // 资源文件大小限制
        maxAssetSize: 250000,

        // 只检查 JS 和 CSS
        assetFilter: (assetFilename) => {
            return assetFilename.endsWith('.js') || assetFilename.endsWith('.css');
        }
    },

    // ==================== 8. 开发工具 ====================

    /**
     * Source Map 配置
     *
     * 常用选项：
     * - false: 不生成 source map
     * - eval: 最快，但信息最少
     * - source-map: 完整独立的 source map 文件
     * - eval-source-map: 开发推荐，包含原始源代码
     * - cheap-module-source-map: 生产推荐，较小
     */
    devtool: isDevelopment ? 'eval-source-map' : 'source-map',

    /**
     * 缓存配置
     */
    cache: {
        type: 'filesystem',  // 使用文件系统缓存
        cacheDirectory: path.resolve(__dirname, '.webpack_cache'),
        buildDependencies: {
            config: [__filename]  // 配置文件变化时使缓存失效
        }
    },

    /**
     * 统计信息
     */
    stats: {
        assets: true,
        chunks: false,
        modules: false,
        entrypoints: false,
        warnings: true,
        errors: true
    }
};

// ==================== 9. 多环境配置导出 ====================

/**
 * 根据环境返回不同配置
 */
module.exports = (env, argv) => {
    // 可以通过命令行参数传入环境变量
    // webpack --env production

    if (env && env.production) {
        // 生产环境特定配置
        return {
            ...config,
            mode: 'production'
        };
    }

    return config;
};

// 默认导出
module.exports = config;
