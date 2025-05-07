for unit in /etc/systemd/system/ngafid.target.wants/*.service; do
  unit_name=$(basename "$unit")
  dropin_dir="/etc/systemd/system/$unit_name.d"
  sudo mkdir -p "$dropin_dir"
  sudo chmod o+r "$dropin_dir"
  sudo ln -sf /opt/ngafid/ngafid2.0/resources/services/restart-policy.conf "$dropin_dir/00-restart.conf"
done
