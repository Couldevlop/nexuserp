{{/* Labels communs */}}
{{- define "nexuserp.labels" -}}
app.kubernetes.io/part-of: nexuserp
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{/* Hôte ingress : <host>.<domain> si host non vide, sinon <domain> */}}
{{- define "nexuserp.ingressHost" -}}
{{- $host := .svc.ingress.host -}}
{{- if $host -}}
{{ $host }}.{{ .root.Values.global.domain }}
{{- else -}}
{{ .root.Values.global.domain }}
{{- end -}}
{{- end -}}
