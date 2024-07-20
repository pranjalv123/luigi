job "luigi" {
  datacenters = ["dc1"]
  type = "service"

  group "luigi" {
    network {
      mode = "bridge"
    }
    service {
      name     = "luigi"
      provider = "consul"
      port = "5555"
      tags     = [
        "traefik.enable=true",
        "traefik.http.routers.luigi.tls.certResolver=vault"
      ]

      connect {
        sidecar_service {
          proxy {
            upstreams {
              destination_name   = "mosquitto"
              local_bind_port    = 1883
              local_bind_address = "127.0.0.1"
            }
          }
        }
      }
    }


    task "luigi" {
      driver = "docker"
      template {
        env = true
        ##        data = <<EOF
        ##OPENWEATHER_API_KEY="{{with secret "kv/data/luigi"}}{{.Data.data.OPENWEATHERMAP_API_KEY}}{{end}}"
        #EOF
        data = <<EOF
OPENWEATHER_API_KEY="{{ with nomadVar "nomad/jobs/luigi" }}{{ .OPENWEATHER_API_KEY }}{{ end }}"
EOF
        destination = "secrets/file.env"
      }
      env {
        LUIGI_ENVIRONMENT = "production"
      }

      config {
        image = "docker-registry.svc.43mar.io/luigi"
      }
      resources {
        cpu    = 800 # 500 MHz
        memory = 512 # 512 MB
      }

    }
  }
}