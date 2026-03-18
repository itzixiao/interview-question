import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers'
import {resolve} from 'path'

export default defineConfig({
    plugins: [
        vue(),
        AutoImport({
            resolvers: [ElementPlusResolver()],
            imports: ['vue', 'vue-router', 'pinia'],
            dts: false
        }),
        Components({
            resolvers: [ElementPlusResolver()],
            dts: false
        })
    ],
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src')
        }
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                // 强制使用 IPv4 地址，避免 Node.js 18+ 将 localhost 解析为 ::1 导致 ECONNREFUSED
                target: 'http://127.0.0.1:8088',
                changeOrigin: true
            }
        }
    }
})
